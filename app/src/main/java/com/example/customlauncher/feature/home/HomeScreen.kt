package com.example.customlauncher.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
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
import com.example.customlauncher.core.ui.pageslider.PageSlider
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
fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 5
        WindowWidthSizeClass.Medium -> 7
        WindowWidthSizeClass.Expanded -> 8
        else -> throw Exception()
    }
    val rows = LocalConfiguration.current.run {
        screenHeightDp / (screenWidthDp / columns) - 2
    }
    var itemHeight by remember { mutableStateOf(0.dp) }

    val state = rememberReorderableLazyGridState(
        onMove = { from, to -> uiState.eventSink(MoveApp(from, to)) },
        canDragOver = { _, _ -> true },
        onDragStart = { _, _, _ -> uiState.eventSink(StartDrag) },
        onDragEnd = { from, to -> uiState.eventSink(StopDrag(from, to)) }
    )

    LaunchedEffect(columns, rows) {
        viewModel.updateGridCount(columns * rows)
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.noRippleClickable { uiState.eventSink(SelectToShowTooltip(null)) }
    ) { paddings ->
        PageSlider(
            pageCount = uiState.appPages.size,
            modifier = Modifier.padding(paddings),
            onHeightChange = { itemHeight = it / rows }
        ) { index ->
            LaunchedEffect(index) {
                viewModel.updateCurrentPage(index)
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(horizontal = 16.dp),
                state = state.gridState,
                modifier = Modifier
                    .fillMaxHeight()
                    .reorderable(state)
            ) {
                homeScreenItems(
                    apps = uiState.appPages[index] ?: emptyList(),
                    selectedPackageName = uiState.selectedApp?.packageName,
                    gridState = state,
                    itemHeight = itemHeight,
                    eventSink = uiState.eventSink,
                )
            }
        }
    }
}

private fun LazyGridScope.homeScreenItems(
    apps: List<UserApp>,
    selectedPackageName: String?,
    gridState: ReorderableLazyGridState,
    itemHeight: Dp,
    eventSink: (HomeScreenEvent) -> Unit,
) {
    items(apps, { it.packageName }) { app ->
        ReorderableItem(
            reorderableState = gridState,
            key = app.packageName,
        ) { isDragging ->
            UserAppItem(
                app = app,
                isSelected = selectedPackageName == app.packageName,
                gridState = gridState,
                isDragging = isDragging,
                eventSink = eventSink,
                modifier = Modifier.height(itemHeight)
            )
        }
    }
}
