package com.example.customlauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.model.App
import com.example.customlauncher.core.model.App.UserApp
import com.example.customlauncher.feature.home.HomeScreenEvent.EditName
import com.example.customlauncher.feature.home.HomeScreenEvent.MoveApp
import com.example.customlauncher.feature.home.HomeScreenEvent.SelectToShowTooltip
import com.example.customlauncher.feature.home.HomeScreenEvent.StartDrag
import com.example.customlauncher.feature.home.HomeScreenEvent.StopDrag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val appPages: Map<Int, List<App>> = emptyMap(),
    val selectedApp: UserApp? = null,
    val eventSink: (HomeScreenEvent) -> Unit = {}
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepo: AppRepository,
) : ViewModel() {

    private val _selectedAppByLongClick = MutableStateFlow<UserApp?>(null)
    private val selected get() = _selectedAppByLongClick.value!!

    private var collectAppsJob: Job? = null
    private var updateAppPositionJob: Job? = null
    private val _appPages = MutableStateFlow<Map<Int, List<App>>>(emptyMap())
    private val appPages get() = _appPages.value

    private val _currentPage = MutableStateFlow(0)

    init {
        startCollect()
    }

    private val eventSink: (HomeScreenEvent) -> Unit = { event ->
        when (event) {
            is SelectToShowTooltip -> _selectedAppByLongClick.value = event.userApp

            is EditName -> viewModelScope.launch {
                appRepo.editAppName(event.value, selected)
                _selectedAppByLongClick.value = null
            }

            is MoveApp -> viewModelScope.launch {
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

            is StartDrag -> cancelAllJobs()

            is StopDrag -> updateAppPositionJob = viewModelScope.launch {
                delay(200)
                appPages[_currentPage.first()]?.let { list ->
                    for (i in minOf(event.from, event.to)..list.lastIndex) {
                        appRepo.moveApp(list[i].packageName, i)
                    }
                    startCollect()
                }
            }
        }
    }

    val uiState = combine(
        _appPages,
        _selectedAppByLongClick,
    ) { pages, selected ->
        HomeUiState(
            appPages = pages,
            selectedApp = selected,
            eventSink = eventSink
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = HomeUiState(),
        started = SharingStarted.WhileSubscribed(5000)
    )

    fun updateGridCount(value: Int) = appRepo.updateGridCount(value)

    fun updateCurrentPage(value: Int) = _currentPage.update { value }

    private fun startCollect() {
        collectAppsJob = combine(
            appRepo.getCompanyAppsStream(),
            appRepo.getUserAppsStream()
        ) { companies, users ->
            _appPages.value = users + (0 to companies)
        }.launchIn(viewModelScope)
    }

    private fun cancelAllJobs() {
        collectAppsJob?.cancel()
        collectAppsJob = null
        updateAppPositionJob?.cancel()
        updateAppPositionJob = null
    }
}