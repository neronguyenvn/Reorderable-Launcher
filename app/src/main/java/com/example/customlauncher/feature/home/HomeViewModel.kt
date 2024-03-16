package com.example.customlauncher.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.customlauncher.core.data.ApplicationRepository
import com.example.customlauncher.core.model.Application
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val applications: List<Application?> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    applicationRepository: ApplicationRepository
) : ViewModel() {

    init {
        viewModelScope.launch { applicationRepository.refreshApplications() }
    }

    val uiState = applicationRepository.getApplicationsStream().map {
        HomeUiState(applications = it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = HomeUiState(),
        started = SharingStarted.WhileSubscribed(5000)
    )
}