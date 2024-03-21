package com.example.customlauncher.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "CompanyApp",
)
data class CompanyAppEntity(
    val name: String,
    val hash: String,
    val pubKey: String,
    val sign: String,
    val version: String,
    val logo: String,
    val orientation: String,
    val author: String,
    val full_screen: Boolean,
    val status_bar: String,
    val type: Int,
    val page: Int,
    val urlWeb: String,
    val isFavorite: Boolean,
    @PrimaryKey
    val id: String,
)