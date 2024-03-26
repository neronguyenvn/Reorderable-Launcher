package com.example.customlauncher.core.data

import com.example.customlauncher.core.datastore.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {

    val userData: Flow<UserPreferences>

    suspend fun setMining(mining: Boolean)
}