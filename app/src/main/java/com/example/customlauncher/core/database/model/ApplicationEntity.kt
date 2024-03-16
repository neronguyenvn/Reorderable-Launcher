package com.example.customlauncher.core.database.model


import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.customlauncher.core.model.Application.CompanyApp
import com.example.customlauncher.core.model.Application.UserApp

@Entity(
    tableName = "Application",
    indices = [Index("packageName", unique = true)]
)
data class ApplicationEntity(
    val name: String,
    val type: ApplicationType,
    val packageName: String? = null,
    val version: String? = null,

    @DrawableRes
    val iconId: Int? = null,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)

enum class ApplicationType { USER, COMPANY }

fun ApplicationEntity.asCompanyApp() = CompanyApp(
    name = name,
    iconId = iconId!!
)

fun ApplicationEntity.asUserApp(icon: Bitmap?): UserApp? = icon?.let {
    UserApp(
        name = name,
        icon = icon,
        packageName = packageName!!,
        version = version!!
    )
}
