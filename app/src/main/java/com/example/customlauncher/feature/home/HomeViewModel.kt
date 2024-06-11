package com.example.customlauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.model.App
import com.example.customlauncher.feature.home.HomeScreenEvent.OnCurrentPageChange
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragMove
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragStart
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragStop
import com.example.customlauncher.feature.home.HomeScreenEvent.OnInit
import com.example.customlauncher.feature.home.HomeScreenEvent.OnNameEditConfirm
import com.example.customlauncher.feature.home.HomeScreenEvent.UpdateMaxAppsPerPage
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
        val isSelecting: Boolean = false
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

    private var collectAppsJob: Job? = null
    private var updateAppPositionJob: Job? = null

    val uiState = combine(
        _appPages,
        _isLoading,
        _isSelecting,
    ) { pages, loading, selecting ->
        if (loading) return@combine Loading
        HomeData(
            appPages = pages,
            isSelecting = selecting
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5000)
    )

    fun onEvent(event: HomeScreenEvent) {
        when (event) {
            is OnInit -> setupInitialState()

            is OnNameEditConfirm -> viewModelScope.launch {
                appRepo.editAppName(event.newName, event.app)
            }

            is OnDragMove -> viewModelScope.launch {
                val currentPage = getCurrentPage()
                val newAppPages = appPages.toMutableList().apply {
                    this[currentPage] = this[currentPage].toMutableList().apply {
                        add(event.to.index, removeAt(event.from.index))
                    }
                }
                _appPages.value = newAppPages
            }

            is OnDragStart -> cancelAllJobs()

            is OnDragStop -> updateAppPositionJob = viewModelScope.launch {
                delay(ITEM_POSITION_SET_DELAY)
                appPages[getCurrentPage()].let { list ->
                    for (i in minOf(event.from, event.to)..maxOf(event.from, event.to)) {
                        appRepo.moveInPage(i, list[i])
                    }
                    startCollect()
                }
            }

            is OnCurrentPageChange -> _currentPage.value = event.value

            is HomeScreenEvent.OnSelectChange -> {
                if (!event.value) {
                    startCollect()
                } else {
                    cancelAllJobs()
                    editAppChecked(true, event.pageIndex!!, event.index!!, true)
                }
                _isSelecting.value = event.value
            }


            is HomeScreenEvent.OnAppCheckChange -> editAppChecked(
                isChecked = event.isChecked,
                pageIndex = event.pageIndex,
                index = event.index,
            )

            is HomeScreenEvent.OnAppMoveConfirm -> viewModelScope.launch {
                val moveApps = _appPages.value.flatMap { list ->
                    list.filter { it.isChecked }
                }
                appRepo.moveToPage(_currentPage.value, moveApps)
                startCollect()
                _isSelecting.value = false
            }

            is UpdateMaxAppsPerPage -> appRepo.
        }
    }

    private fun setupInitialState() = viewModelScope.launch {
        appRepo.refreshApps()
        startCollect()
        _isLoading.value = false
    }

    private fun startCollect() {
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
