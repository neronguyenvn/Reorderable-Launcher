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
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ItemPosition
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ReorderableItem
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ReorderableLazyGridState
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.rememberReorderableLazyGridState
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.reorderable
import com.example.customlauncher.core.designsystem.util.noRippleClickable
import com.example.customlauncher.core.model.App.UserApp
import com.example.customlauncher.core.ui.appitem.UserAppItem
import com.example.customlauncher.feature.home.HomeScreenEvent.MoveApp
import com.example.customlauncher.feature.home.HomeScreenEvent.SelectToShowTooltip
import com.example.customlauncher.feature.home.HomeScreenEvent.StartDrag
import com.example.customlauncher.feature.home.HomeScreenEvent.StopDrag

sealed interface HomeScreenEvent {
    data class SelectToShowTooltip(val userApp: UserApp?) : HomeScreenEvent
    data class EditName(val value: String) : HomeScreenEvent
    data class MoveApp(val from: ItemPosition, val to: ItemPosition) : HomeScreenEvent
    data object StartDrag : HomeScreenEvent
    data class StopDrag(val from: Int, val to: Int) : HomeScreenEvent
}

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = rememberReorderableLazyGridState(
        onMove = { from, to -> uiState.eventSink(MoveApp(from, to)) },
        canDragOver = { _, _ -> true },
        onDragStart = { _, _, _ -> uiState.eventSink(StartDrag) },
        onDragEnd = { from, to -> uiState.eventSink(StopDrag(from, to)) }
    )

    Box(
        Modifier
            .fillMaxSize()
            .noRippleClickable { uiState.eventSink(SelectToShowTooltip(null)) }
            .statusBarsPadding()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            state = state.gridState,
            modifier = Modifier.reorderable(state)
        ) {
            homeScreenItems(
                apps = uiState.apps,
                selectedAppPackageName = uiState.selectedApp?.packageName,
                gridState = state,
                eventSink = uiState.eventSink
            )
        }
    }
}

private fun LazyGridScope.homeScreenItems(
    apps: List<UserApp>,
    selectedAppPackageName: String?,
    gridState: ReorderableLazyGridState,
    eventSink: (HomeScreenEvent) -> Unit,
) {
    items(apps, { it.packageName }) { app ->
        ReorderableItem(gridState, app.packageName) { isDragging ->
            UserAppItem(
                app = app,
                isSelected = selectedAppPackageName == app.packageName,
                gridState = gridState,
                isDragging = isDragging,
                eventSink = eventSink
            )
        }
    }
}
