package com.example.customlauncher.core.ui.webview

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.customlauncher.feature.home.HomeScreenEvent

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ClWebView(
    url: String,
    modifier: Modifier = Modifier,
    onEvent: (HomeScreenEvent) -> Unit
) {

    BackHandler {
        onEvent(HomeScreenEvent.OnWebDataChange(null))
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        }
    )
}