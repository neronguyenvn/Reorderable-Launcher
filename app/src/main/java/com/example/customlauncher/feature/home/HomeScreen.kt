package com.example.customlauncher.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.customlauncher.core.ui.pageslider.PageSlider
import com.example.customlauncher.feature.home.HomeScreenEvent.OnCurrentPageChange
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragMove
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragStart
import com.example.customlauncher.feature.home.HomeScreenEvent.OnDragStop
import com.example.customlauncher.feature.home.HomeScreenEvent.OnGridCountChange
import com.example.customlauncher.feature.home.HomeScreenEvent.OnInitialSetup
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
}

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
    var itemHeight by remember { mutableStateOf(0.dp) }

    val state = rememberReorderableLazyGridState(
        onMove = { from, to -> viewModel.onEvent(OnDragMove(from, to)) },
        canDragOver = { _, _ -> true },
        onDragStart = { _, _, _ -> viewModel.onEvent(OnDragStart) },
        onDragEnd = { from, to -> viewModel.onEvent(OnDragStop(from, to)) }

    )

    when (uiState) {
        is HomeUiState.Loading -> LoadingEffect()
        is HomeUiState.HomeData -> Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.noRippleClickable { viewModel.onEvent(OnUserAppLongClick(null)) }
        ) { paddings ->
            PageSlider(
                pageCount = (uiState as HomeUiState.HomeData).appPages.size,
                modifier = Modifier.padding(paddings),
                onHeightChange = { itemHeight = it / rows },
                onPageChange = { viewModel.onEvent(OnCurrentPageChange(it)) }
            ) { index ->
                HomeDataUi(
                    uiState = uiState as HomeUiState.HomeData,
                    columns = columns,
                    index = index,
                    itemHeight = itemHeight,
                    state = state,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
private fun HomeDataUi(
    uiState: HomeUiState.HomeData,
    columns: Int,
    index: Int,
    itemHeight: Dp,
    state: ReorderableLazyGridState,
    onEvent: (HomeScreenEvent) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(horizontal = 16.dp),
        state = state.gridState,
        modifier = Modifier
            .fillMaxSize()
            .reorderable(state)
    ) {
        homeScreenItems(
            apps = uiState.appPages[index] ?: emptyList(),
            selectedPackageName = uiState.selectedApp?.packageName,
            gridState = state,
            itemHeight = itemHeight,
            onEvent = { onEvent(it) }
        )
    }
}

private fun LazyGridScope.homeScreenItems(
    apps: List<App>,
    selectedPackageName: String?,
    gridState: ReorderableLazyGridState,
    itemHeight: Dp,
    onEvent: (HomeScreenEvent) -> Unit,
) {
    items(apps, { it.packageName }) { app ->
        ReorderableItem(
            reorderableState = gridState,
            key = app.packageName,
        ) { isDragging ->
            when (app) {
                is UserApp -> UserAppItem(
                    app = app,
                    isSelected = selectedPackageName == app.packageName,
                    gridState = gridState,
                    isDragging = isDragging,
                    modifier = Modifier.height(itemHeight),
                    onEvent = onEvent
                )

                is App.CompanyApp -> CompanyAppItem(
                    app = app,
                    gridState = gridState,
                    modifier = Modifier.height(itemHeight)
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