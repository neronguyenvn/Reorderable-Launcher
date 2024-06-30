package com.example.customlauncher.core.ui.appitem

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.customlauncher.core.designsystem.util.conditional
import com.example.customlauncher.core.model.App
import com.example.customlauncher.core.model.TooltipMenu
import com.example.customlauncher.core.model.launch
import com.example.customlauncher.core.model.showInfo
import com.example.customlauncher.core.model.uninstall
import com.example.customlauncher.feature.home.HomeEvent
import com.example.customlauncher.feature.home.HomeEvent.OnAppCheckChange
import com.example.customlauncher.feature.home.HomeEvent.OnDragStart
import com.example.customlauncher.feature.home.HomeEvent.OnDragStop
import com.example.customlauncher.feature.home.HomeEvent.OnNameEditConfirm
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableLazyGridState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppItem(
    app: App,
    isUiMoving: Boolean,
    pageIndex: Int,
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
                changeToMovingUi = {
                    onEvent(
                        HomeEvent.OnSelectChange(
                            true,
                            pageIndex,
                            index
                        )
                    )
                },
                cancelSelected = { tooltipState.dismiss() }
            )
        }) {
        AppItemUi(
            app = app,
            isUiMoving = isUiMoving,
            modifier = with(reorderableScope) {
                modifier
                    .conditional(isUiMoving,
                        ifTrue = {
                            clickable {
                                onEvent(
                                    OnAppCheckChange(
                                        !app.isChecked,
                                        pageIndex,
                                        index
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
            onEvent(OnAppCheckChange(it, pageIndex, index))
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
    isUiMoving: Boolean,
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
        modifier = modifier.fillMaxSize()
    ) {
        BadgedBox(
            badge = {
                when {
                    isUiMoving -> Checkbox(
                        checked = app.isChecked,
                        onCheckedChange = { onItemSelect(it) }
                    )

                    app.notificationCount != 0 -> Badge(modifier = Modifier.size(20.dp)) {
                        Text(text = (app.notificationCount).toString())
                    }
                }
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
            color = Color.White,
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
    changeToMovingUi: () -> Unit,
    cancelSelected: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .clip(RoundedCornerShape(15))
            .background(Color.White)
    ) {
        TooltipMenuItem(tooltipMenu = TooltipMenu.Edit) {
            showEditNameDialog()
        }
        HorizontalDivider()
        TooltipMenuItem(tooltipMenu = TooltipMenu.AppInfo) {
            app.showInfo(context)
            cancelSelected()
        }
        HorizontalDivider()
        TooltipMenuItem(tooltipMenu = TooltipMenu.Move) {
            changeToMovingUi()
            cancelSelected()
        }
        if (app.canUninstall) {
            HorizontalDivider()
            TooltipMenuItem(tooltipMenu = TooltipMenu.Uninstall) {
                app.uninstall(context)
                cancelSelected()
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