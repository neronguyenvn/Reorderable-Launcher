package com.example.customlauncher.core.database.model


import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.customlauncher.core.database.ApplicationDao
import com.example.customlauncher.core.model.Application.UserApp

@Entity(
    tableName = "UserApp",
    indices = [Index("packageName", unique = true)]
)
data class UserAppEntity(
    val name: String,
    val version: String,

    @PrimaryKey
    val packageName: String
)

fun UserAppEntity.asUserApp(icon: Bitmap?): UserApp? = icon?.let {
    UserApp(
        name = name,
        icon = icon,
        packageName = packageName,
        version = version
    )
}

suspend fun UserAppEntity.isInstalledAndUpToDate(dao: ApplicationDao): Boolean {
    val installed = dao.getByPackageName(packageName) ?: return false
    return installed.version == version
}