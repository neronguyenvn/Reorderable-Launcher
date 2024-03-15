package com.example.customlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val flags = PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
        val activities: List<ResolveInfo> = packageManager.queryIntentActivities(intent, flags)

        val installedApps = activities.map { resolveInfo ->
            LauncherApp.SystemApp(
                name = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(packageManager)
            )
        }

        val list = installedApps + LauncherApp.CompanyApp(
            icon = R.drawable.ic_launcher_foreground,
            name = "Android"
        )

        setContent {
            Surface(
                Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                var loadde by remember { mutableStateOf(false) }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list) {
                        when (it) {
                            is LauncherApp.CompanyApp -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { loadde = true }
                                ) {
                                    Image(
                                        painter = painterResource(id = it.icon),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(0.4f)
                                            .aspectRatio(1f)
                                            .background(Green, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = it.name, modifier = Modifier.weight(0.6f))
                                }
                            }

                            is LauncherApp.SystemApp -> {
                                Row(
                                    Modifier.clickable { it.launch(this@MainActivity) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val width = it.icon!!.intrinsicWidth
                                    val height = it.icon.intrinsicHeight
                                    val bitmap =
                                        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bitmap)
                                    it.icon.setBounds(0, 0, canvas.width, canvas.height)
                                    it.icon.draw(canvas)
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .weight(0.4f)
                                            .aspectRatio(1f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = it.name, modifier = Modifier.weight(0.6f))
                                }
                            }
                        }
                    }
                }

                if (loadde) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = true
                                webChromeClient = WebChromeClient()
                                loadUrl("https://google.com.vn")
                            }
                        }
                    )
                }
            }
        }
    }
}

sealed interface LauncherApp {

    data class CompanyApp(
        @DrawableRes
        val icon: Int,
        val name: String
    ) : LauncherApp

    data class SystemApp(
        val name: String,
        val packageName: String,
        val icon: Drawable?
    ) : LauncherApp
}

fun LauncherApp.SystemApp.launch(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    context.startActivity(intent)
}