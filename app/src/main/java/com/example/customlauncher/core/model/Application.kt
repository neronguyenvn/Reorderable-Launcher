package com.example.customlauncher.core.model

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import androidx.annotation.DrawableRes


sealed class Application {

    abstract val packageName: String

    data class CompanyApp(
        val name: String,
        override val packageName: String,

        @DrawableRes
        val iconId: Int
    ) : Application()

    data class UserApp(
        val name: String,
        override val packageName: String,
        val version: String,
        val icon: Bitmap,
        val canUninstall: Boolean,
        val notificationCount: Int
    ) : Application()
}

fun Application.UserApp.launch(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    context.startActivity(intent)
}

fun Application.UserApp.showInfo(context: Context) {
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        context.startActivity(this)
    }
}

fun Application.UserApp.uninstall(context: Context) {
    Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:$packageName")
        context.startActivity(this)
    }
}
