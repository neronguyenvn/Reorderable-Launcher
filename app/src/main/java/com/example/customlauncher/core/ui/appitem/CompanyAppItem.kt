package com.example.customlauncher.core.ui.appitem

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.ReorderableLazyGridState
import com.example.customlauncher.core.designsystem.component.reorderablelazygrid.detectPressOrDragAndReorder
import com.example.customlauncher.core.designsystem.util.conditional
import com.example.customlauncher.core.model.App
import com.example.customlauncher.core.model.TooltipMenu
import com.example.customlauncher.feature.home.HomeScreenEvent
import com.example.customlauncher.feature.home.HomeScreenEvent.OnItemCheck
import com.example.customlauncher.feature.home.HomeScreenEvent.OnMoveSelect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyAppItem(
    app: App.CompanyApp,
    gridState: ReorderableLazyGridState,
    pageIndex: Int,
    index: Int,
    isDragging: Boolean,
    isMovingUi: Boolean,
    modifier: Modifier = Modifier,
    onEvent: (HomeScreenEvent) -> Unit
) {
    var showWebView by remember { mutableStateOf(false) }
    val tooltipState = rememberTooltipState()
    val coroutineScope = rememberCoroutineScope()

    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(app.logo)
        .memoryCacheKey(app.packageName)
        .build()

    LaunchedEffect(isDragging) {
        if (isDragging) {
            tooltipState.dismiss()
        }
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = tooltipState,
        enableUserInput = false,
        tooltip = {
            TooltipBoxUi(
                changeToMovingUi = { onEvent(OnMoveSelect(true, pageIndex, index)) },
                cancelSelected = { tooltipState.dismiss() }
            )
        }) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(15))
                .conditional(
                    isMovingUi,
                    ifFalse = {
                        detectPressOrDragAndReorder(
                            state = gridState,
                            onClick = { showWebView = true },
                            onLongClick = { coroutineScope.launch { tooltipState.show() } }
                        )
                    },
                    ifTrue = {
                        clickable { onEvent(OnItemCheck(!app.isChecked, pageIndex, index)) }
                    }
                )
        ) {
            BadgedBox(badge = {
                if (isMovingUi) {
                    Checkbox(
                        checked = app.isChecked,
                        onCheckedChange = {
                            onEvent(OnItemCheck(it, pageIndex, index))
                        }
                    )
                }
            }) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.7f)
                )
            }
            Text(
                text = app.name, style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }

    if (showWebView) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    loadUrl(app.urlWeb)
                }
            }
        )
    }
}

@Composable
private fun TooltipBoxUi(
    changeToMovingUi: () -> Unit,
    cancelSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .clip(RoundedCornerShape(15))
            .background(Color.White)
    ) {
        TooltipMenuItem(tooltipMenu = TooltipMenu.Move) {
            changeToMovingUi()
            cancelSelected()
        }
    }
}