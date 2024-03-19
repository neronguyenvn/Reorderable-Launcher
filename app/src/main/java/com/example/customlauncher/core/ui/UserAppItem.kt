package com.example.customlauncher.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.customlauncher.core.model.Application
import com.example.customlauncher.core.model.TooltipMenu
import com.example.customlauncher.core.model.launch
import com.example.customlauncher.core.model.showInfo
import com.example.customlauncher.core.model.uninstall
import com.example.customlauncher.feature.home.HomeScreenEvent


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAppItem(
    app: Application.UserApp,
    isSelected: Boolean,
    eventSink: (HomeScreenEvent) -> Unit
) {
    val tooltipState = rememberTooltipState()
    var showEditNameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isSelected) {
        if (isSelected) tooltipState.show() else tooltipState.dismiss()
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        state = tooltipState,
        tooltip = {
            TooltipBoxUi(
                app = app,
                showEditNameDialog = { showEditNameDialog = true },
                cancelSelected = { eventSink(HomeScreenEvent.LongClickOnApp(null)) },
            )
        }
    ) {
        AppItemUi(app) {
            eventSink(HomeScreenEvent.LongClickOnApp(it))
        }
    }

    if (showEditNameDialog) {
        EditAppNameDialog(
            currentName = app.name,
            dismiss = { showEditNameDialog = false }
        ) { newName ->
            eventSink(HomeScreenEvent.EditName(newName))
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AppItemUi(
    app: Application.UserApp,
    selectApp: (Application.UserApp) -> Unit,
) {
    val context = LocalContext.current
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(app.icon)
        .memoryCacheKey(app.packageName)
        .build()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(15))
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = { selectApp(app) },
                onClick = { app.launch(context) }
            )
            .padding(vertical = 4.dp),
    ) {
        BadgedBox(
            badge = {
                androidx.compose.animation.AnimatedVisibility(app.notificationCount != 0) {
                    Badge(modifier = Modifier.size(20.dp)) {
                        Text(text = (app.notificationCount).toString())
                    }
                }
            },
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name, style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TooltipBoxUi(
    app: Application.UserApp,
    showEditNameDialog: () -> Unit,
    cancelSelected: () -> Unit
) {
    val context = LocalContext.current
    Column(
        Modifier
            .background(Color.White, RoundedCornerShape(15))
            .fillMaxWidth(0.5f)
    ) {
        TooltipMenuItem(tooltipMenu = TooltipMenu.Edit) {
            showEditNameDialog()
        }
        HorizontalDivider()
        TooltipMenuItem(tooltipMenu = TooltipMenu.AppInfo) {
            app.showInfo(context)
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