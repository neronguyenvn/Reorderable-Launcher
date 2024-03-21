package com.example.customlauncher.feature.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.customlauncher.R
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ItemPosition
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ReorderableItem
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ReorderableLazyGridState
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.detectReorderAfterLongPress
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.rememberReorderableLazyGridState
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.reorderable
import com.example.customlauncher.core.designsystem.util.noRippleClickable
import com.example.customlauncher.core.model.Application
import com.example.customlauncher.core.model.Application.UserApp
import com.example.customlauncher.core.ui.UserAppItem
import com.example.customlauncher.feature.home.HomeScreenEvent.LongClickOnApp
import com.example.customlauncher.feature.home.HomeScreenEvent.MoveApp
import com.example.customlauncher.feature.home.HomeScreenEvent.StartDrag
import com.example.customlauncher.feature.home.HomeScreenEvent.StopDrag
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifImageView

sealed interface HomeScreenEvent {
    data class LongClickOnApp(val userApp: UserApp?) : HomeScreenEvent
    data class EditName(val value: String) : HomeScreenEvent
    data class MoveApp(val from: ItemPosition, val to: ItemPosition) : HomeScreenEvent
    data object StartDrag : HomeScreenEvent
    data class StopDrag(val from: Int, val to: Int) : HomeScreenEvent
}

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDataLoaded by viewModel.isDataLoaded.collectAsState()
    val state = rememberReorderableLazyGridState(
        onMove = { from, to -> uiState.eventSink(MoveApp(from, to)) },
        canDragOver = { _, _ -> true },
        onDragStart = { _, _, _ -> uiState.eventSink(StartDrag) },
        onDragEnd = { from, to -> uiState.eventSink(StopDrag(from, to)) }
    )
    if (!isDataLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            //CircularProgressIndicator()
            // Tạo GifDrawable từ tệp tin GIF
            val gifInputStream = LocalContext.current.resources.openRawResource(R.raw.loading)
            val gifDrawable = GifDrawable(gifInputStream)

            // Sử dụng GifImageViewWrapper để hiển thị GifImageView
            GifImageViewWrapper(
                gifDrawable = gifDrawable,
                modifier = Modifier.size(50.dp)
            )

        }
    } else {
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
                state = state.gridState,
                modifier = Modifier.reorderable(state)
            ) {
                homeScreenItems(
                    applications = uiState.applications,
                    selectedAppPackageName = uiState.selectedApplication?.packageName,
                    gridState = state,
                    eventSink = uiState.eventSink
                )
            }
        }
    }

}

private fun LazyGridScope.homeScreenItems(
    applications: List<Application>,
    selectedAppPackageName: String?,
    gridState: ReorderableLazyGridState,
    eventSink: (HomeScreenEvent) -> Unit,
) {
    items(applications, { it.packageName }) { app ->
        app as UserApp
        ReorderableItem(gridState, app.packageName) { isDragging ->
            val elevation = animateDpAsState(if (isDragging) 8.dp else 0.dp)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .detectReorderAfterLongPress(gridState)
                    .shadow(elevation.value)
            ) {
                UserAppItem(
                    app = app,
                    isSelected = selectedAppPackageName == app.packageName,
                    eventSink = eventSink
                )
            }
        }
    }
}
@Composable
fun GifImageViewWrapper(
    gifDrawable: GifDrawable,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AndroidView(factory = { context ->
            GifImageView(context).apply {
                setImageDrawable(gifDrawable)
            }
        })
    }
}
