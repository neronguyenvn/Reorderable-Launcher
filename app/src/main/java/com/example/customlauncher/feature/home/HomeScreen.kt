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
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.customlauncher.core.model.App
import com.example.customlauncher.core.ui.appitem.AppItem
import com.example.customlauncher.core.ui.pageslider.PageIndicator
import com.example.customlauncher.feature.home.HomeEvent.OnAppMoveConfirm
import com.example.customlauncher.feature.home.HomeEvent.OnCurrentPageChange
import com.example.customlauncher.feature.home.HomeEvent.OnDragMove
import com.example.customlauncher.feature.home.HomeEvent.OnInit
import com.example.customlauncher.feature.home.HomeEvent.OnSelectingToMoveChange
import com.example.customlauncher.feature.home.HomeEvent.UpdateMaxAppsPerPage
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyGridState
import kotlin.math.roundToInt

private const val COLUMNS = 5

sealed interface HomeEvent {

    data object OnInit : HomeEvent

    data class OnNameEditConfirm(
        val newName: String,
        val app: App
    ) : HomeEvent

    data class OnDragMove(val from: Int, val to: Int) : HomeEvent

    data object OnDragStart : HomeEvent

    data object OnDragStop : HomeEvent

    data class OnCurrentPageChange(val value: Int) : HomeEvent

    data class OnSelectingToMoveChange(
        val selectingToMove: Boolean,
        val page: Int? = null,
        val index: Int? = null
    ) : HomeEvent

    data class OnAppCheckChange(
        val checked: Boolean,
        val page: Int,
        val index: Int
    ) : HomeEvent

    data object OnAppMoveConfirm : HomeEvent

    data class UpdateMaxAppsPerPage(val count: Int) : HomeEvent
}

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

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddings ->
        val paddingModifier = Modifier.padding(paddings)
        when (uiState) {
            is HomeUiState.Loading -> LoadingEffect(paddingModifier)
            is HomeUiState.HomeData -> {
                BackHandler { Unit }

                val uiDataState = uiState as HomeUiState.HomeData
                val pagerState = rememberPagerState { uiDataState.apps.size }

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.collect { page ->
                        viewModel.onEvent(OnCurrentPageChange(page))
                    }
                }

                Column(paddingModifier) {
                    AnimatedVisibility(visible = uiDataState.selectingToMove) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Button(onClick = { viewModel.onEvent(OnSelectingToMoveChange(false)) }) {
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
                        modifier = Modifier.weight(1f),
                        beyondBoundsPageCount = 1,
                        flingBehavior = PagerDefaults.flingBehavior(
                            state = pagerState,
                            snapPositionalThreshold = 0.2f
                        )
                    ) {
                        val lazyGridState = rememberLazyGridState()
                        val reorderableLazyGridState = rememberReorderableLazyGridState(
                            lazyGridState
                        ) { from, to ->
                            viewModel.onEvent(OnDragMove(from.index, to.index))
                        }

                        AppGridUi(
                            uiState = uiDataState,
                            rows = rows,
                            pageIndex = it,
                            state = lazyGridState,
                            reorderableState = reorderableLazyGridState,
                            showTooltip = uiDataState.shouldShowTooltipOnLongPress,
                            onEvent = viewModel::onEvent
                        )
                    }
                    PageIndicator(
                        index = pagerState.currentPage,
                        count = uiDataState.apps.size,
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
    state: LazyGridState,
    reorderableState: ReorderableLazyGridState,
    showTooltip: Boolean,
    onEvent: (HomeEvent) -> Unit
) {
    val density = LocalDensity.current
    var itemHeight by remember { mutableStateOf(0.dp) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        contentPadding = PaddingValues(horizontal = 16.dp),
        state = state,
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                itemHeight = with(density) {
                    it.size.height.toDp() / rows
                }
            }
    ) {
        homeScreenItems(
            apps = uiState.apps[pageIndex],
            reorderableState = reorderableState,
            itemHeight = itemHeight,
            isMovingUi = uiState.selectingToMove,
            pageIndex = pageIndex,
            showTooltip = showTooltip,
            onEvent = onEvent
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyGridScope.homeScreenItems(
    apps: List<App>,
    reorderableState: ReorderableLazyGridState,
    itemHeight: Dp,
    isMovingUi: Boolean,
    pageIndex: Int,
    showTooltip: Boolean,
    onEvent: (HomeEvent) -> Unit,
) {
    itemsIndexed(
        items = apps,
        key = { _, item -> item.packageName }
    ) { index, app ->
        ReorderableItem(
            state = reorderableState,
            key = app.packageName,
            modifier = Modifier.height(itemHeight)
        ) { _ ->
            AppItem(
                app = app,
                isUiMoving = isMovingUi,
                pageIndex = pageIndex,
                index = index,
                reorderableScope = this,
                reorderableState = reorderableState,
                showTooltip = showTooltip,
                onEvent = onEvent,
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
