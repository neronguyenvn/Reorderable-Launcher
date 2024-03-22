package com.example.customlauncher.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.customlauncher.R
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ItemPosition
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ReorderableItem
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ReorderableLazyGridState
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.rememberReorderableLazyGridState
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.reorderable
import com.example.customlauncher.core.designsystem.util.noRippleClickable
import com.example.customlauncher.core.model.App
import com.example.customlauncher.core.model.App.UserApp
import com.example.customlauncher.core.ui.appitem.CompanyAppItem
import com.example.customlauncher.core.ui.appitem.UserAppItem
import com.example.customlauncher.core.ui.pageslider.PageIndicator
import com.example.customlauncher.feature.home.HomeScreenEvent.OnCurrentPageChange
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragMove
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragStart
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragStop
import com.example.customlauncher.feature.home.HomeScreenEvent.OnGridCountChange
import com.example.customlauncher.feature.home.HomeScreenEvent.OnInitialSetup
import com.example.customlauncher.feature.home.HomeScreenEvent.OnMovingSelect
import com.example.customlauncher.feature.home.HomeScreenEvent.OnUserAppLongClick
import kotlin.math.roundToInt

sealed interface HomeScreenEvent {
    data object OnInitialSetup : HomeScreenEvent
    data class OnUserAppLongClick(val userApp: UserApp?) : HomeScreenEvent
    data class OnEditNameConfirm(val value: String) : HomeScreenEvent
    data class OnDragMove(val from: ItemPosition, val to: ItemPosition) : HomeScreenEvent
    data object OnDragStart : HomeScreenEvent
    data class OnDragStop(val from: Int, val to: Int) : HomeScreenEvent
    data class OnGridCountChange(val value: Int) : HomeScreenEvent
    data class OnCurrentPageChange(val value: Int) : HomeScreenEvent
    data class OnMovingSelect(val value: Boolean) : HomeScreenEvent
    data class OnItemCheck(val isChecked: Boolean, val pageIndex: Int, val index: Int) :
        HomeScreenEvent
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    viewModel: HomeViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.onEvent(OnInitialSetup)
    }

    val columns = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> 5
        WindowWidthSizeClass.Medium -> 7
        WindowWidthSizeClass.Expanded -> 8
        else -> throw Exception()
    }

    val rows = LocalConfiguration.current.run {
        screenHeightDp / (screenWidthDp * 1.5 / columns).roundToInt()
    }

    LaunchedEffect(columns, rows) {
        viewModel.onEvent(OnGridCountChange(columns * rows))
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = rememberReorderableLazyGridState(
        onMove = { from, to -> viewModel.onEvent(OnDragMove(from, to)) },
        onDragStart = { _, _, _ -> viewModel.onEvent(OnDragStart) },
        onDragEnd = { from, to -> viewModel.onEvent(OnDragStop(from, to)) }
    )

    when (uiState) {
        is HomeUiState.Loading -> LoadingEffect()
        is HomeUiState.HomeData -> Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.noRippleClickable { viewModel.onEvent(OnUserAppLongClick(null)) }
        ) { paddings ->
            val uiDataState = uiState as HomeUiState.HomeData
            val pagerState = rememberPagerState { uiDataState.appPages.size }

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    viewModel.onEvent(OnCurrentPageChange(page))
                }
            }

            Column(Modifier.padding(paddings)) {
                AnimatedVisibility(visible = uiDataState.isMoving) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Button(onClick = { viewModel.onEvent(OnMovingSelect(false)) }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = { /*TODO*/ }) {
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
                        columns = columns,
                        rows = rows,
                        pageIndex = it,
                        state = state,
                        onEvent = viewModel::onEvent
                    )
                }
                PageIndicator(
                    index = pagerState.currentPage,
                    count = uiDataState.appPages.size,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun AppGridUi(
    uiState: HomeUiState.HomeData,
    columns: Int,
    rows: Int,
    pageIndex: Int,
    state: ReorderableLazyGridState,
    onEvent: (HomeScreenEvent) -> Unit
) {
    val density = LocalDensity.current
    var itemHeight by remember { mutableStateOf(0.dp) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
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
            apps = uiState.appPages[pageIndex] ?: emptyList(),
            selectedPackageName = uiState.selectedApp?.packageName,
            gridState = state,
            itemHeight = itemHeight,
            isMovingUi = uiState.isMoving,
            pageIndex = pageIndex,
            onEvent = onEvent
        )
    }
}

private fun LazyGridScope.homeScreenItems(
    apps: List<App>,
    selectedPackageName: String?,
    gridState: ReorderableLazyGridState,
    itemHeight: Dp,
    isMovingUi: Boolean,
    pageIndex: Int,
    onEvent: (HomeScreenEvent) -> Unit,
) {
    itemsIndexed(apps, { _: Int, item: App -> item.packageName }) { index, app ->
        ReorderableItem(
            reorderableState = gridState,
            key = app.packageName,
            modifier = Modifier.height(itemHeight)
        ) { isDragging ->
            when (app) {
                is UserApp -> UserAppItem(
                    app = app,
                    isSelected = selectedPackageName == app.packageName,
                    gridState = gridState,
                    isDragging = isDragging,
                    isUiMoving = isMovingUi,
                    pageIndex = pageIndex,
                    index = index,
                    onEvent = onEvent
                )

                is App.CompanyApp -> CompanyAppItem(
                    app = app,
                    gridState = gridState,
                    pageIndex = pageIndex,
                    index = index,
                    isDragging = isDragging,
                    isMovingUi = isMovingUi,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun LoadingEffect() {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.loading))
    val progress by animateLottieCompositionAsState(composition)
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.fillMaxSize()
    )
}