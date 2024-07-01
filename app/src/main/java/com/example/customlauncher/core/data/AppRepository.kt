package com.example.customlauncher.core.data

import com.example.customlauncher.core.model.App
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    fun getAppsStream(): Flow<List<List<App>>>

    suspend fun refreshApps()

    suspend fun editAppName(newName: String, app: App)

    suspend fun moveInPage(toIndex: Int, app: App)

    suspend fun moveToPage(toPage: Int, apps: List<App>)

    suspend fun updateMaxAppsPerPage(count: Int)
}
