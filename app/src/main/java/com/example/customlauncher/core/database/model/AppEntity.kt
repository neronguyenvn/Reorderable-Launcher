package com.example.customlauncher.core.database.model

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.customlauncher.core.model.App

@Entity(tableName = "UserApp")
data class AppEntity(
    val name: String,
    val version: String,
    val index: Int,
    val page: Int,

    @PrimaryKey
    val packageName: String
)

fun AppEntity.asExternalModel(
    icon: Bitmap?,
    canUninstall: Boolean
) = icon?.let {
    App(
        name = name,
        icon = icon,
        packageName = packageName,
        version = version,
        canUninstall = canUninstall,
        index = index
    )
}

fun AppEntity.isInstalledAndUpToDate(dbApps: Map<String, AppEntity>): Boolean {
    val installed = dbApps[packageName] ?: return false
    return installed.version == version
}

fun AppEntity.canUninstall(packageManager: PackageManager): Boolean {
    return try {
        packageManager.getApplicationInfo(packageName, 0)
            .flags and ApplicationInfo.FLAG_SYSTEM == 0
    } catch (e: NameNotFoundException) {
        false
    }
}
