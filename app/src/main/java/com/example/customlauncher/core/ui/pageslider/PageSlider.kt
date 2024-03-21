package com.example.customlauncher.core.ui.pageslider

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PageSlider(
    pageCount: Int,
    modifier: Modifier = Modifier,
    onPageChange: (Int) -> Unit,
    onHeightChange: (Dp) -> Unit,
    content: @Composable () -> Unit
) {
    val pagerState = rememberPagerState { pageCount }
    val density = LocalDensity.current

    LaunchedEffect(pagerState) {
        onPageChange(pagerState.currentPage)
    }

    Column(modifier) {
        HorizontalPager(
            state = pagerState,
            pageContent = { content() },
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned {
                    onHeightChange(with(density) {
                        it.size.height.toDp()
                    })
                }
        )
        PageIndicator(
            index = pagerState.currentPage,
            count = pageCount,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}