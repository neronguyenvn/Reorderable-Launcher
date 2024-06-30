package com.example.customlauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.model.App
import com.example.customlauncher.feature.home.HomeEvent.OnAppCheckChange
import com.example.customlauncher.feature.home.HomeEvent.OnAppMoveConfirm
import com.example.customlauncher.feature.home.HomeEvent.OnCurrentPageChange
import com.example.customlauncher.feature.home.HomeEvent.OnDragMove
import com.example.customlauncher.feature.home.HomeEvent.OnDragStart
import com.example.customlauncher.feature.home.HomeEvent.OnDragStop
import com.example.customlauncher.feature.home.HomeEvent.OnInit
import com.example.customlauncher.feature.home.HomeEvent.OnNameEditConfirm
import com.example.customlauncher.feature.home.HomeEvent.OnSelectChange
import com.example.customlauncher.feature.home.HomeEvent.UpdateMaxAppsPerPage
import com.example.customlauncher.feature.home.HomeUiState.HomeData
import com.example.customlauncher.feature.home.HomeUiState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ITEM_POSITION_SET_DELAY = 400L

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class HomeData(
        val appPages: List<List<App>> = emptyList(),
        val isSelecting: Boolean = false,
        val shouldShowTooltipOnLongPress: Boolean = true
    ) : HomeUiState
}


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepo: AppRepository,
) : ViewModel() {
    private val _appPages = MutableStateFlow<List<List<App>>>(emptyList())
    private val appPages get() = _appPages.value

    private val _currentPage = MutableStateFlow(0)
    private suspend fun getCurrentPage() = _currentPage.first()

    private val _isLoading = MutableStateFlow(true)
    private val _isSelecting = MutableStateFlow(false)
    private val _shouldShowTooltipOnLongPress = MutableStateFlow(true)

    private var collectAppsJob: Job? = null
    private var updateAppPositionJob: Job? = null

    private var tempFrom: Int? = null
    private var tempTo: Int? = null

    val uiState = combine(
        _appPages,
        _isLoading,
        _isSelecting,
        _shouldShowTooltipOnLongPress
    ) { pages, loading, selecting, should ->
        if (loading) return@combine Loading
        HomeData(
            appPages = pages,
            isSelecting = selecting,
            shouldShowTooltipOnLongPress = should
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5000)
    )

    fun onEvent(event: HomeEvent) {
        when (event) {
            is OnInit -> setupInitialState()

            is OnNameEditConfirm -> viewModelScope.launch {
                appRepo.editAppName(event.newName, event.app)
            }

            is OnDragMove -> viewModelScope.launch {
                val currentPage = getCurrentPage()
                tempFrom = event.from
                tempTo = event.to

                val newAppPages = appPages.toMutableList().apply {
                    this[currentPage] = this[currentPage].toMutableList().apply {
                        add(event.to, removeAt(event.from))
                    }
                }
                _appPages.value = newAppPages

                _shouldShowTooltipOnLongPress.value = false
            }

            is OnDragStart -> cancelAllJobs()

            is OnDragStop -> {
                if (tempFrom == null || tempTo == null) {
                    return
                }

                updateAppPositionJob?.cancel()
                updateAppPositionJob = viewModelScope.launch {
                    delay(ITEM_POSITION_SET_DELAY)

                    val start = minOf(tempFrom!!, tempTo!!)
                    val end = maxOf(tempFrom!!, tempTo!!)

                    tempFrom = null
                    tempTo = null

                    appPages[getCurrentPage()].let { list ->
                        for (i in start..end) {
                            appRepo.moveInPage(i, list[i])
                        }
                        startCollect()
                    }
                }

                _shouldShowTooltipOnLongPress.value = true
            }

            is OnCurrentPageChange -> _currentPage.value = event.value

            is OnSelectChange -> {
                if (!event.value) {
                    startCollect()
                } else {
                    cancelAllJobs()
                    editAppChecked(true, event.pageIndex!!, event.index!!, true)
                }
                _isSelecting.value = event.value
            }


            is OnAppCheckChange -> editAppChecked(
                isChecked = event.isChecked,
                pageIndex = event.pageIndex,
                index = event.index,
            )

            is OnAppMoveConfirm -> viewModelScope.launch {
                val moveApps = _appPages.value.flatMap { list ->
                    list.filter { it.isChecked }
                }
                appRepo.moveToPage(_currentPage.value, moveApps)
                startCollect()
                _isSelecting.value = false
            }

            is UpdateMaxAppsPerPage -> viewModelScope.launch {
                appRepo.updateMaxAppsPerPage(event.count)
            }
        }
    }

    private fun setupInitialState() = viewModelScope.launch {
        appRepo.refreshApps()
        startCollect()
        _isLoading.value = false
    }

    private fun startCollect() {
        collectAppsJob?.cancel()
        collectAppsJob = appRepo.getAppsStream()
            .onEach { _appPages.value = it }
            .launchIn(viewModelScope)
    }

    private fun cancelAllJobs() {
        collectAppsJob?.cancel()
        collectAppsJob = null
        updateAppPositionJob?.cancel()
        updateAppPositionJob = null
    }

    private fun editAppChecked(
        isChecked: Boolean,
        pageIndex: Int,
        index: Int,
        shouldCreateNewPage: Boolean = false
    ) {
        val newAppPages = appPages.toMutableList().apply {
            if (shouldCreateNewPage) add(emptyList())
            this[pageIndex] = this[pageIndex].toMutableList().apply {
                this[index] = this[index].copy(isChecked = isChecked)
            }
        }
        _appPages.value = newAppPages
    }
}
