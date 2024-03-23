package com.example.customlauncher.core.data

import android.service.notification.StatusBarNotification
import com.example.customlauncher.core.model.App
import com.example.customlauncher.core.model.App.UserApp
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    fun getAppsStream(): Flow<Map<Int, List<App>>>

    fun updateGridCount(value: Int)

    suspend fun refreshUserApps()

    suspend fun refreshCompanyApps()

    suspend fun editAppName(newName: String, app: UserApp)

    suspend fun handleNotis(notifications: List<StatusBarNotification>)

    suspend fun moveInPage(toIndex: Int, app: App)

    suspend fun moveToPage(toPage: Int, apps: List<App>)
}