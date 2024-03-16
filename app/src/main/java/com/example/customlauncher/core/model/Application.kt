package com.example.customlauncher.core.model

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes


sealed interface Application {

    data class CompanyApp(
        val name: String,

        @DrawableRes
        val iconId: Int
    ) : Application

    data class UserApp(
        val name: String,
        val packageName: String,
        val version: String,
        val icon: Bitmap
    ) : Application
}

fun Application.UserApp.launch(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    context.startActivity(intent)
}