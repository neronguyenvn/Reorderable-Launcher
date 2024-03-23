package com.example.customlauncher.core.model

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import com.example.customlauncher.core.model.App.UserApp


sealed class App {

    abstract val packageName: String
    abstract val isChecked: Boolean
    abstract val index: Int

    data class CompanyApp(
        val name: String,
        val version: String,
        val urlWeb: String,
        val logo: String,
        val type: Long,
        override val packageName: String,
        override val index: Int,
        override val isChecked: Boolean = false,
    ) : App()

    data class UserApp(
        val name: String,
        val version: String,
        val icon: Bitmap,
        val canUninstall: Boolean,
        val notificationCount: Int,
        override val packageName: String,
        override val index: Int,
        override val isChecked: Boolean = false,
    ) : App()

    fun editChecked(value: Boolean): App {
        return when (this) {
            is UserApp -> this.copy(isChecked = value)
            is CompanyApp -> this.copy(isChecked = value)
        }
    }
}

fun UserApp.launch(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    context.startActivity(intent)
}

fun UserApp.showInfo(context: Context) {
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
        context.startActivity(this)
    }
}

fun UserApp.uninstall(context: Context) {
    Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:$packageName")
        context.startActivity(this)
    }
}