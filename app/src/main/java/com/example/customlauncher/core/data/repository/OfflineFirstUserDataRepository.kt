package com.example.customlauncher.core.data.repository

import com.example.customlauncher.core.data.UserDataRepository
import com.example.customlauncher.core.datastore.ClPreferencesDataSource
import com.example.customlauncher.core.datastore.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineFirstUserDataRepository @Inject constructor(
    private val clPreferencesDataSource: ClPreferencesDataSource
) : UserDataRepository {

    override val userData: Flow<UserPreferences> = clPreferencesDataSource.userData

    override suspend fun setMining(mining: Boolean) =
        clPreferencesDataSource.setMining(mining)
}