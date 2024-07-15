package com.neronguyenvn.reorderablelauncher.core.model

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings

data class App(
    val name: String,
    val version: String,
    val icon: Bitmap,
    val canUninstall: Boolean,
    val packageName: String,
    val index: Int,
    val checked: Boolean = false,
)

fun App.launch(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    context.startActivity(intent)
}

fun App.showInfo(context: Context) {
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        context.startActivity(this)
    }
}

fun App.uninstall(context: Context) {
    Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:$packageName")
        context.startActivity(this)
    }
}
