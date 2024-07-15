package com.neronguyenvn.reorderablelauncher.core.ui.appitem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.neronguyenvn.reorderablelauncher.core.designsystem.util.conditional
import com.neronguyenvn.reorderablelauncher.core.model.App
import com.neronguyenvn.reorderablelauncher.core.model.TooltipMenu
import com.neronguyenvn.reorderablelauncher.core.model.launch
import com.neronguyenvn.reorderablelauncher.core.model.showInfo
import com.neronguyenvn.reorderablelauncher.core.model.uninstall
import com.neronguyenvn.reorderablelauncher.feature.home.HomeEvent
import com.neronguyenvn.reorderablelauncher.feature.home.HomeEvent.OnAppCheck
import com.neronguyenvn.reorderablelauncher.feature.home.HomeEvent.OnDragStart
import com.neronguyenvn.reorderablelauncher.feature.home.HomeEvent.OnDragStop
import com.neronguyenvn.reorderablelauncher.feature.home.HomeEvent.OnNameEditConfirm
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableLazyGridState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppItem(
    app: App,
    isUiMoving: Boolean,
    page: Int,
    index: Int,
    reorderableScope: ReorderableCollectionItemScope,
    reorderableState: ReorderableLazyGridState,
    modifier: Modifier = Modifier,
    onEvent: (HomeEvent) -> Unit,
    showTooltip: Boolean,
) {
    val tooltipState = rememberTooltipState()
    var showEditNameDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(showTooltip) {
        if (!showTooltip) {
            tooltipState.dismiss()
        }
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = tooltipState,
        enableUserInput = false,
        tooltip = {
            TooltipBoxUi(
                app = app,
                showEditNameDialog = { showEditNameDialog = true },
                dismissTooltip = { tooltipState.dismiss() },
                selectToMove = {
                    onEvent(
                        HomeEvent.OnSelectingToMove(
                            selectingToMove = true,
                            page = page,
                            index = index
                        )
                    )
                },
            )
        }) {
        AppItemUi(
            app = app,
            selectingToMove = isUiMoving,
            modifier = with(reorderableScope) {
                modifier
                    .fillMaxSize()
                    .conditional(isUiMoving,
                        ifTrue = {
                            clickable {
                                onEvent(
                                    OnAppCheck(
                                        checked = !app.checked,
                                        page = page,
                                        index = index
                                    )
                                )
                            }
                        },
                        ifFalse = {
                            pointerInput(reorderableState) {
                                detectTapGestures(
                                    onTap = { app.launch(context) },
                                    onLongPress = { coroutineScope.launch { tooltipState.show() } },
                                )
                            }
                        }
                    )
                    .longPressDraggableHandle(
                        onDragStarted = { onEvent(OnDragStart) },
                        onDragStopped = { onEvent(OnDragStop) }
                    )
            }
        ) {
            onEvent(OnAppCheck(it, page, index))
        }
    }

    if (showEditNameDialog) {
        EditAppNameDialog(
            currentName = app.name,
            dismiss = { showEditNameDialog = false }
        ) { newName ->
            onEvent(OnNameEditConfirm(newName, app))
        }
    }
}

@Composable
private fun AppItemUi(
    app: App,
    selectingToMove: Boolean,
    modifier: Modifier = Modifier,
    onItemSelect: (Boolean) -> Unit
) {
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(app.icon)
        .memoryCacheKey(app.packageName)
        .build()

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        BadgedBox(
            badge = {
                if (selectingToMove) Checkbox(
                    checked = app.checked,
                    onCheckedChange = { onItemSelect(it) }
                )
            },
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.7f)
            )
        }
        Text(
            text = app.name, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

@Composable
private fun TooltipBoxUi(
    app: App,
    showEditNameDialog: () -> Unit,
    selectToMove: () -> Unit,
    dismissTooltip: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .clip(RoundedCornerShape(15))
            .background(MaterialTheme.colorScheme.secondary)
    ) {
        TooltipMenuItem(tooltipMenu = TooltipMenu.Edit) {
            showEditNameDialog()
        }
        HorizontalDivider()
        TooltipMenuItem(tooltipMenu = TooltipMenu.AppInfo) {
            app.showInfo(context)
            dismissTooltip()
        }
        HorizontalDivider()
        TooltipMenuItem(tooltipMenu = TooltipMenu.Move) {
            selectToMove()
            dismissTooltip()
        }
        if (app.canUninstall) {
            HorizontalDivider()
            TooltipMenuItem(tooltipMenu = TooltipMenu.Uninstall) {
                app.uninstall(context)
                dismissTooltip()
            }
        }
    }
}

@Composable
private fun EditAppNameDialog(
    currentName: String,
    dismiss: () -> Unit,
    editName: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    Dialog(onDismissRequest = { dismiss() }) {
        Card {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Input Name") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { dismiss() }) {
                        Text(text = "CANCEL")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = {
                        editName(newName)
                        dismiss()
                    }) {
                        Text(text = "OK")
                    }
                }
            }
        }
    }
}