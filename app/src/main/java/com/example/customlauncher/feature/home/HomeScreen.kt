package com.example.customlauncher.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ItemPosition
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ReorderableItem
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ReorderableLazyGridState
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.rememberReorderableLazyGridState
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.reorderable
import com.example.customlauncher.core.model.App
import com.example.customlauncher.core.ui.appitem.AppItem
import com.example.customlauncher.core.ui.pageslider.PageIndicator
import com.example.customlauncher.feature.home.HomeEvent.OnAppMoveConfirm
import com.example.customlauncher.feature.home.HomeEvent.OnCurrentPageChange
import com.example.customlauncher.feature.home.HomeEvent.OnDragMove
import com.example.customlauncher.feature.home.HomeEvent.OnDragStart
import com.example.customlauncher.feature.home.HomeEvent.OnDragStop
import com.example.customlauncher.feature.home.HomeEvent.OnInit
import com.example.customlauncher.feature.home.HomeEvent.OnSelectChange
import com.example.customlauncher.feature.home.HomeEvent.UpdateMaxAppsPerPage
import kotlin.math.roundToInt

sealed interface HomeEvent {

    data object OnInit : HomeEvent

    data class OnNameEditConfirm(
        val newName: String,
        val app: App
    ) : HomeEvent

    data class OnDragMove(val from: ItemPosition, val to: ItemPosition) : HomeEvent

    data object OnDragStart : HomeEvent

    data class OnDragStop(val from: Int, val to: Int) : HomeEvent

    data class OnCurrentPageChange(val value: Int) : HomeEvent

    data class OnSelectChange(
        val value: Boolean,
        val pageIndex: Int? = null,
        val index: Int? = null
    ) : HomeEvent

    data class OnAppCheckChange(val isChecked: Boolean, val pageIndex: Int, val index: Int) :
        HomeEvent

    data object OnAppMoveConfirm : HomeEvent

    data class UpdateMaxAppsPerPage(val count: Int) : HomeEvent
}

private const val COLUMNS = 5

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.onEvent(OnInit)
    }

    val rows = LocalConfiguration.current.run {
        screenHeightDp / (screenWidthDp * 1.5 / COLUMNS).roundToInt()
    }
    LaunchedEffect(rows) {
        viewModel.onEvent(UpdateMaxAppsPerPage(COLUMNS * rows))
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = rememberReorderableLazyGridState(
        onMove = { from, to -> viewModel.onEvent(OnDragMove(from, to)) },
        onDragStart = { _, _, _ -> viewModel.onEvent(OnDragStart) },
        onDragEnd = { from, to -> viewModel.onEvent(OnDragStop(from, to)) }
    )

    Scaffold(containerColor = Color.LightGray) { paddings ->
        val paddingModifier = Modifier.padding(paddings)
        when (uiState) {
            is HomeUiState.Loading -> LoadingEffect(paddingModifier)
            is HomeUiState.HomeData -> {
                BackHandler { Unit }

                val uiDataState = uiState as HomeUiState.HomeData
                val pagerState = rememberPagerState { uiDataState.appPages.size }

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.collect { page ->
                        viewModel.onEvent(OnCurrentPageChange(page))
                    }
                }

                Column(paddingModifier) {
                    AnimatedVisibility(visible = uiDataState.isSelecting) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Button(onClick = { viewModel.onEvent(OnSelectChange(false)) }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Button(onClick = { viewModel.onEvent(OnAppMoveConfirm) }) {
                                Text("Move Here")
                            }
                        }
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) {
                        AppGridUi(
                            uiState = uiDataState,
                            rows = rows,
                            pageIndex = it,
                            state = state,
                            onEvent = viewModel::onEvent
                        )
                    }
                    PageIndicator(
                        index = pagerState.currentPage,
                        count = uiDataState.appPages.size,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppGridUi(
    uiState: HomeUiState.HomeData,
    rows: Int,
    pageIndex: Int,
    state: ReorderableLazyGridState,
    onEvent: (HomeEvent) -> Unit
) {
    val density = LocalDensity.current
    var itemHeight by remember { mutableStateOf(0.dp) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        contentPadding = PaddingValues(horizontal = 16.dp),
        state = state.gridState,
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                itemHeight = with(density) {
                    it.size.height.toDp() / rows
                }
            }
            .reorderable(state)
    ) {
        homeScreenItems(
            apps = uiState.appPages[pageIndex],
            gridState = state,
            itemHeight = itemHeight,
            isMovingUi = uiState.isSelecting,
            pageIndex = pageIndex,
            onEvent = onEvent
        )
    }
}

private fun LazyGridScope.homeScreenItems(
    apps: List<App>,
    gridState: ReorderableLazyGridState,
    itemHeight: Dp,
    isMovingUi: Boolean,
    pageIndex: Int,
    onEvent: (HomeEvent) -> Unit,
) {
    itemsIndexed(
        items = apps,
        contentType = { _: Int, item: App -> item.packageName }
    ) { index, app ->
        ReorderableItem(
            reorderableState = gridState,
            key = app.packageName,
            modifier = Modifier.height(itemHeight)
        ) { isDragging ->
            AppItem(
                app = app,
                gridState = gridState,
                isDragging = isDragging,
                isUiMoving = isMovingUi,
                pageIndex = pageIndex,
                index = index,
                onEvent = onEvent
            )
        }
    }
}

@Composable
private fun LoadingEffect(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        LinearProgressIndicator(Modifier.align(Alignment.Center))
    }
}
