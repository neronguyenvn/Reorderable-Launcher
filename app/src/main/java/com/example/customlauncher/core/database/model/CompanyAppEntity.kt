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
    val isFavorite: Boolean,

    @PrimaryKey
    val id: String
)

fun CompanyAppEntity.asExternalModel() = CompanyApp(
    name = name,
    version = version,
    urlWeb = urlWeb,
    logo = logo,
    type = type,
    isFavorite = isFavorite,
    packageName = id
)