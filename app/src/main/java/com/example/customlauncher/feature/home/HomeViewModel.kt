package com.example.customlauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.model.App
import com.example.customlauncher.core.model.App.UserApp
import com.example.customlauncher.feature.home.HomeScreenEvent.OnCurrentPageChange
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragMove
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragStart
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragStop
import com.example.customlauncher.feature.home.HomeScreenEvent.OnGridCountChange
import com.example.customlauncher.feature.home.HomeScreenEvent.OnInitialSetup
import com.example.customlauncher.feature.home.HomeScreenEvent.OnNameEditConfirm
import com.example.customlauncher.feature.home.HomeScreenEvent.OnUserAppLongClick
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
        val appPages: Map<Int, List<App>> = emptyMap(),
        val selectedApp: UserApp? = null,
        val isMoving: Boolean = false
    ) : HomeUiState
}


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepo: AppRepository,
) : ViewModel() {

    private val _selectedByLongClick = MutableStateFlow<UserApp?>(null)
    private val selectedByLongClick get() = _selectedByLongClick.value!!

    private val _appPages = MutableStateFlow<Map<Int, List<App>>>(emptyMap())
    private val appPages get() = _appPages.value

    private val _currentPage = MutableStateFlow(0)
    private val _isLoading = MutableStateFlow(true)
    private val _isMovingSelect = MutableStateFlow(false)

    private var collectAppsJob: Job? = null
    private var updateAppPositionJob: Job? = null

    val uiState = combine(
        _appPages,
        _selectedByLongClick,
        _isLoading,
        _isMovingSelect
    ) { pages, selected, loading, isMoving ->
        if (loading) return@combine Loading
        HomeData(
            appPages = pages,
            selectedApp = selected,
            isMoving = isMoving
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5000)
    )

    fun onEvent(event: HomeScreenEvent) {
        when (event) {
            is OnInitialSetup -> setupInitialState()

            is OnUserAppLongClick -> _selectedByLongClick.value = event.userApp

            is OnNameEditConfirm -> viewModelScope.launch {
                appRepo.editAppName(event.value, selectedByLongClick)
                _selectedByLongClick.value = null
            }

            is OnDragMove -> viewModelScope.launch {
                val currentPage = _currentPage.first()
                val newAppPages = appPages.mapValues {
                    if (it.key == currentPage) {
                        it.value.toMutableList().apply {
                            add(event.to.index, removeAt(event.from.index))
                        }
                    } else it.value
                }
                _appPages.value = newAppPages
            }

            is OnDragStart -> cancelAllJobs()

            is OnDragStop -> updateAppPositionJob = viewModelScope.launch {
                delay(ITEM_POSITION_SET_DELAY)
                appPages[_currentPage.first()]?.let { list ->
                    for (i in minOf(event.from, event.to)..maxOf(event.from, event.to)) {
                        appRepo.moveInPage(i, list[i])
                    }
                    startCollect()
                }
            }

            is OnGridCountChange -> appRepo.updateGridCount(event.value)

            is OnCurrentPageChange -> _currentPage.value = event.value

            is HomeScreenEvent.OnMoveSelect -> {
                _isMovingSelect.value = event.value
                if (!event.value) {
                    val newAppPages = appPages.mapValues {
                        it.value.map { app -> app.editChecked(false) }
                    }
                    _appPages.value = newAppPages
                }
            }

            is HomeScreenEvent.OnItemCheck -> {
                val newAppPages = appPages.mapValues {
                    if (it.key == event.pageIndex) {
                        it.value.toMutableList().apply {
                            this[event.index] = this[event.index].editChecked(event.isChecked)
                        }
                    } else it.value
                }
                _appPages.value = newAppPages
            }

            is HomeScreenEvent.OnMoveConfirm -> viewModelScope.launch {
                val moveApps = _appPages.value.values.flatMap { list ->
                    list.filter { it.isChecked }
                }
                appRepo.moveToPage(_currentPage.value, moveApps)
                val newAppPages = appPages.mapValues {
                    it.value.map { app -> app.editChecked(false) }
                }
                _appPages.value = newAppPages
                _isMovingSelect.value = false
            }

        }
    }

    private fun setupInitialState() = viewModelScope.launch {
        appRepo.refreshCompanyApps()
        appRepo.refreshUserApps()
        startCollect()
        _isLoading.value = false
    }

    private fun startCollect() {
        collectAppsJob = appRepo.getAppsStream()
            .onEach {
                val appPages = it
                _appPages.value = appPages
            }
            .launchIn(viewModelScope)
    }

    private fun cancelAllJobs() {
        collectAppsJob?.cancel()
        collectAppsJob = null
        updateAppPositionJob?.cancel()
        updateAppPositionJob = null
    }
}