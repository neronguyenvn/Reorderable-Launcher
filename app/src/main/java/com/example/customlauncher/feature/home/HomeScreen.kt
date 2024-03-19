package com.example.customlauncher.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.customlauncher.core.model.Application
import com.example.customlauncher.core.model.Application.CompanyApp
import com.example.customlauncher.core.model.Application.UserApp
import com.example.customlauncher.core.ui.CompanyAppItem
import com.example.customlauncher.core.ui.UserAppItem
import com.example.customlauncher.core.ui.util.noRippleClickable
import com.example.customlauncher.feature.home.HomeScreenEvent.LongClickOnApp

sealed interface HomeScreenEvent {
    data class LongClickOnApp(val userApp: UserApp?) : HomeScreenEvent
    data class EditName(val value: String) : HomeScreenEvent
}

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Box(
        Modifier
            .fillMaxSize()
            .noRippleClickable { uiState.eventSink(LongClickOnApp(null)) }
            .statusBarsPadding()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            homeScreenItems(
                applications = uiState.applications,
                selectedAppPackageName = uiState.selectedApplication?.packageName,
                eventSink = uiState.eventSink
            )
        }
    }
}

private fun LazyGridScope.homeScreenItems(
    applications: List<Application?>,
    selectedAppPackageName: String?,
    eventSink: (HomeScreenEvent) -> Unit,
) {
    items(applications) { app ->
        when (app) {
            is CompanyApp -> CompanyAppItem(app)
            null -> Unit
            is UserApp -> UserAppItem(
                app = app,
                isSelected = selectedAppPackageName == app.packageName,
                eventSink = eventSink
            )
        }
    }
}
