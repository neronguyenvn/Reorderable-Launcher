package com.example.customlauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customlauncher.core.data.ApplicationRepository
import com.example.customlauncher.core.model.Application
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val applications: List<Application?> = emptyList(),
    val selectedApplication: Application.UserApp? = null,
    val eventSink: (HomeScreenEvent) -> Unit = {}
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    appRepo: ApplicationRepository
) : ViewModel() {


    private val eventSink: (HomeScreenEvent) -> Unit = { event ->
        when (event) {
            is HomeScreenEvent.LongClickOnApp -> _selectedApp.value = event.userApp
            is HomeScreenEvent.EditName -> viewModelScope.launch {
                appRepo.editName(event.value, selected)
                _selectedApp.value = null
            }
        }
    }

    private val _selectedApp = MutableStateFlow<Application.UserApp?>(null)
    private val selected get() = _selectedApp.value!!

    val uiState = appRepo.getApplicationsStream().combine(_selectedApp) { apps, selected ->
        HomeUiState(
            applications = apps,
            selectedApplication = selected,
            eventSink = eventSink
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = HomeUiState(),
        started = SharingStarted.WhileSubscribed(5000)
    )
}