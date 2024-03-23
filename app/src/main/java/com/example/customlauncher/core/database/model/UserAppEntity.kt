package com.example.customlauncher.core.database.model


import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.customlauncher.core.model.App.UserApp

@Entity(tableName = "UserApp")
data class UserAppEntity(
    val name: String,
    val version: String,
    val index: Int,
    val page: Int,
    val usageMillis: Long = 0,
    val notificationCount: Int = 0,

    @PrimaryKey
    val packageName: String
)

fun UserAppEntity.asExternalModel(
    icon: Bitmap?,
    canUninstall: Boolean
): UserApp? = icon?.let {
    UserApp(
        name = name,
        icon = icon,
        packageName = packageName,
        version = version,
        canUninstall = canUninstall,
        notificationCount = notificationCount,
        index = index
    )
}

fun UserAppEntity.isInstalledAndUpToDate(map: Map<String, UserAppEntity>): Boolean {
    val installed = map[packageName] ?: return false
    return installed.version == version
}

fun UserAppEntity.canUninstall(packageManager: PackageManager): Boolean {
    return try {
        packageManager.getApplicationInfo(packageName, 0)
            .flags and ApplicationInfo.FLAG_SYSTEM == 0
    } catch (e: NameNotFoundException) {
        false
    }
}