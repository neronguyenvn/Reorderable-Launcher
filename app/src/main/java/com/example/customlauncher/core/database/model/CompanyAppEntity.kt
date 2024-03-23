package com.example.customlauncher.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.customlauncher.core.model.App.CompanyApp

@Entity(tableName = "CompanyApp")
data class CompanyAppEntity(
    val name: String,
    val version: String,
    val urlWeb: String,
    val logo: String,
    val type: Long,
    val index: Int,
    val page: Int,

    @PrimaryKey
    val packageName: String
)

fun CompanyAppEntity.asExternalModel() = CompanyApp(
    name = name,
    version = version,
    urlWeb = urlWeb,
    logo = logo,
    type = type,
    packageName = packageName,
    index = index
)

fun CompanyAppEntity.isInstalledAndUpToDate(map: Map<String, CompanyAppEntity>): Boolean {
    val installed = map[packageName] ?: return false
    return installed.version == version
}