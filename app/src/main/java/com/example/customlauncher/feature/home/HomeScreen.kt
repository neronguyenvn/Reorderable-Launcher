package com.example.customlauncher.feature.home

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.customlauncher.core.model.Application
import com.example.customlauncher.core.model.Application.CompanyApp
import com.example.customlauncher.core.model.Application.UserApp
import com.example.customlauncher.core.model.TooltipMenu
import com.example.customlauncher.core.model.launch
import com.example.customlauncher.core.model.showInfo
import com.example.customlauncher.core.model.uninstall
import com.example.customlauncher.core.ui.TooltipMenuItem
import com.example.customlauncher.core.ui.util.noRippleClickable
import com.example.customlauncher.feature.home.HomeScreenEvent.EditName
import com.example.customlauncher.feature.home.HomeScreenEvent.LongClickOnApp

sealed interface HomeScreenEvent {
    data class LongClickOnApp(val userApp: UserApp?) : HomeScreenEvent
    data class EditName(val value: String) : HomeScreenEvent
}

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Box(
        Modifier
            .fillMaxSize()
            .noRippleClickable { uiState.eventSink(LongClickOnApp(null)) }) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.background(Color.Transparent)
        ) {
            homeScreenItems(
                applications = uiState.applications,
                selectedAppPackageName = uiState.selectedApplication?.packageName,
                eventSink = uiState.eventSink
            )
        }
    }
}

private fun LazyGridScope.homeScreenItems(
    applications: List<Application?>,
    selectedAppPackageName: String?,
    eventSink: (HomeScreenEvent) -> Unit,
) {
    items(applications) { app ->
        when (app) {
            is CompanyApp -> CompanyAppItem(app)
            is UserApp -> UserAppItem(
                app = app,
                isSelected = selectedAppPackageName == app.packageName,
                eventSink = eventSink
            )

            null -> Unit
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun UserAppItem(
    app: UserApp,
    isSelected: Boolean,
    eventSink: (HomeScreenEvent) -> Unit
) {
    val context = LocalContext.current
    val imageRequest = ImageRequest.Builder(context)
        .data(app.icon)
        .memoryCacheKey(app.packageName)
        .build()

    val tooltipState = rememberTooltipState()
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    LaunchedEffect(isSelected) {
        if (isSelected) tooltipState.show() else tooltipState.dismiss()
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            Column(
                Modifier
                    .background(Color.White, RoundedCornerShape(15))
                    .fillMaxWidth(0.5f)
            ) {
                TooltipMenuItem(tooltipMenu = TooltipMenu.Edit) {
                    showEditNameDialog = true
                    newName = app.name
                }
                HorizontalDivider()
                TooltipMenuItem(tooltipMenu = TooltipMenu.AppInfo) {
                    app.showInfo(context)
                    eventSink(LongClickOnApp(null))
                }
                if (app.canUninstall) {
                    HorizontalDivider()
                    TooltipMenuItem(tooltipMenu = TooltipMenu.Uninstall) {
                        app.uninstall(context)
                        eventSink(LongClickOnApp(null))
                    }
                }
            }
        },
        state = tooltipState,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(15))
                .fillMaxWidth()
                .combinedClickable(
                    onLongClick = { eventSink(LongClickOnApp(app)) },
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

    if (showEditNameDialog) {
        Dialog(onDismissRequest = { showEditNameDialog = false }) {
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
                        Button(onClick = { showEditNameDialog = false }) {
                            Text(text = "CANCEL")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = {
                            eventSink(EditName(newName))
                            showEditNameDialog = false
                        }) {
                            Text(text = "OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyAppItem(app: CompanyApp) {
    var loadde by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(15))
            .clickable { loadde = true }
    ) {
        Image(
            painter = painterResource(id = app.iconId),
            contentDescription = null,
            modifier = Modifier
                .aspectRatio(1f)
                .background(Color.Green, CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            color = Color.White,
        )
    }

    if (loadde) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    loadUrl("https://google.com.vn")
                }
            }
        )
    }
}