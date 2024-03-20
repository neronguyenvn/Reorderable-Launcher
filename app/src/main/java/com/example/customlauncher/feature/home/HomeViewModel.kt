package com.example.customlauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customlauncher.core.data.AppRepository
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val apps: List<UserApp> = emptyList(),
    val selectedApp: UserApp? = null,
    val eventSink: (HomeScreenEvent) -> Unit = {}
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepo: AppRepository
) : ViewModel() {

    private val _selectedApp = MutableStateFlow<UserApp?>(null)
    private val selected get() = _selectedApp.value!!

    private var collectAppsJob: Job? = null
    private var updateAppPositionJob: Job? = null
    private val _applications = MutableStateFlow<List<UserApp>>(emptyList())
    private val apps get() = _applications.value

    init {
        startCollect()
    }

    private val eventSink: (HomeScreenEvent) -> Unit = { event ->
        when (event) {
            is SelectToShowTooltip -> _selectedApp.value = event.userApp

            is EditName -> viewModelScope.launch {
                appRepo.editAppName(event.value, selected)
                _selectedApp.value = null
            }

            is MoveApp -> _applications.value = apps.toMutableList()
                .apply { add(event.to.index, removeAt(event.from.index)) }

            is StartDrag -> cancelAllJobs()

            is StopDrag -> updateAppPositionJob = viewModelScope.launch {
                delay(500)
                for (i in minOf(event.from, event.to)..apps.lastIndex) {
                    appRepo.moveApp(apps[i].packageName, i)
                }
                startCollect()
            }
        }
    }

    val uiState = _applications.combine(_selectedApp) { apps, selected ->
        HomeUiState(
            apps = apps,
            selectedApp = selected,
            eventSink = eventSink
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = HomeUiState(),
        started = SharingStarted.WhileSubscribed(5000)
    )

    private fun startCollect() {
        collectAppsJob = appRepo.getAppsStream()
            .onEach { _applications.value = it.filterNotNull() }
            .launchIn(viewModelScope)
    }

    private fun cancelAllJobs() {
        collectAppsJob?.cancel()
        collectAppsJob = null
        updateAppPositionJob?.cancel()
        updateAppPositionJob = null
    }
}