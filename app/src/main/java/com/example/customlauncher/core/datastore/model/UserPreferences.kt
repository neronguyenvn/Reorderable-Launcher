package com.example.customlauncher.core.datastore.model

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val isMiningOn: Boolean = false
)
