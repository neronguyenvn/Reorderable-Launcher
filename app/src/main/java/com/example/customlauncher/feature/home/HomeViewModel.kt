package com.example.customlauncher.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.model.App
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

private const val ITEM_POSITION_SET_DELAY_MILLIS = 300L
private const val TAG = "HomeViewModel"

sealed interface HomeUiState {

    data object Loading : HomeUiState

    data class HomeData(
        val apps: List<List<App>> = emptyList(),
        val selectingToMove: Boolean = false,
        val shouldShowTooltipOnLongPress: Boolean = true
    ) : HomeUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepo: AppRepository,
) : ViewModel() {

    private val _apps = MutableStateFlow<List<List<App>>>(emptyList())
    private val apps get() = _apps.value

    private val _currentPage = MutableStateFlow(0)
    private suspend fun getCurrentPage() = _currentPage.first()

    private val _isLoading = MutableStateFlow(true)
    private val _selectingToMove = MutableStateFlow(false)
    private val _shouldShowTooltipOnLongPress = MutableStateFlow(true)

    private var subscribeAppsStreamJob: Job? = null
    private var updateAppPositionJob: Job? = null

    private var moveRange: IntRange? = null

    val uiState = combine(
        _apps,
        _isLoading,
        _selectingToMove,
        _shouldShowTooltipOnLongPress
    ) { apps, loading, selecting, should ->
        if (loading) {
            return@combine HomeUiState.Loading
        }
        HomeUiState.HomeData(
            apps = apps,
            selectingToMove = selecting,
            shouldShowTooltipOnLongPress = should
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = HomeUiState.Loading,
        started = SharingStarted.WhileSubscribed(5000)
    )

    fun onEvent(event: HomeEvent) {
        when (event) {

            is HomeEvent.OnInit -> setupInitialState()

            is HomeEvent.OnNameEditConfirm -> viewModelScope.launch {
                appRepo.editAppName(event.newName, event.app)
            }

            is HomeEvent.OnDragMove -> viewModelScope.launch {

                val currentPage = getCurrentPage()

                _apps.value = apps.mapIndexed { page, appsInPage ->
                    if (page == currentPage) {
                        appsInPage.toMutableList().apply {
                            add(event.to, removeAt(event.from))
                        }
                    } else appsInPage
                }

                moveRange = moveRange?.let {
                    IntRange(
                        minOf(event.to, event.from, it.first),
                        maxOf(event.to, event.from, it.last)
                    )
                } ?: IntRange(minOf(event.to, event.from), maxOf(event.to, event.from))

                Log.d(TAG, "MoveRange: $moveRange")
                _shouldShowTooltipOnLongPress.value = false
            }

            is HomeEvent.OnDragStart -> cancelAllJobs()

            is HomeEvent.OnDragStop -> {
                if (moveRange == null) {
                    subscribeAppsStream()
                    return
                }

                updateAppPositionJob?.cancel()
                updateAppPositionJob = viewModelScope.launch {

                    delay(ITEM_POSITION_SET_DELAY_MILLIS)

                    apps[getCurrentPage()].let { list ->
                        for (i in moveRange!!) {
                            appRepo.moveInPage(i, list[i])
                        }
                    }

                    moveRange = null
                    subscribeAppsStream()
                }

                _shouldShowTooltipOnLongPress.value = true
            }

            is HomeEvent.OnCurrentPageChange -> _currentPage.value = event.value

            is HomeEvent.OnSelectingToMove -> {
                if (event.selectingToMove) {
                    editAppChecked(
                        checked = true,
                        page = event.page!!,
                        index = event.index!!,
                    )
                    _apps.value = apps.toMutableList().apply { add(emptyList()) }
                } else {
                    clearLastEmptyPage()
                }
                _selectingToMove.value = event.selectingToMove
            }


            is HomeEvent.OnAppCheck -> editAppChecked(
                checked = event.checked,
                page = event.page,
                index = event.index,
            )

            is HomeEvent.OnAppMoveConfirm -> viewModelScope.launch {

                _selectingToMove.value = false
                cancelAllJobs()

                val moveApps = _apps.value.flatMap { list ->
                    list.filter { it.checked }
                }
                appRepo.moveToPage(_currentPage.value, moveApps)

                subscribeAppsStream()
            }

            is HomeEvent.UpdateMaxAppsPerPage -> viewModelScope.launch {
                appRepo.updateMaxAppsPerPage(event.count)
            }
        }
    }

    private fun setupInitialState() = viewModelScope.launch {
        appRepo.refreshApps()
        subscribeAppsStream()
        _isLoading.value = false
    }

    private fun subscribeAppsStream() {
        subscribeAppsStreamJob?.cancel()
        subscribeAppsStreamJob = appRepo.getAppsStream()
            .onEach { _apps.value = it }
            .launchIn(viewModelScope)
    }

    private fun cancelAllJobs() {
        subscribeAppsStreamJob?.cancel()
        subscribeAppsStreamJob = null
        updateAppPositionJob?.cancel()
        updateAppPositionJob = null
    }

    private fun editAppChecked(
        checked: Boolean,
        page: Int,
        index: Int,
    ) {
        val newAppPages = apps.toMutableList().apply {
            this[page] = this[page].toMutableList().apply {
                this[index] = this[index].copy(checked = checked)
            }
        }
        _apps.value = newAppPages
    }

    private fun clearLastEmptyPage() {
        val newAppPages = apps.toMutableList().apply {
            if (last().isEmpty()) {
                removeLastOrNull()
            }
        }
        _apps.value = newAppPages
    }
}
