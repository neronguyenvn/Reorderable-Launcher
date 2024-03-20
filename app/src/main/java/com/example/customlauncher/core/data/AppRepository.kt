package com.example.customlauncher.core.data

import android.service.notification.StatusBarNotification
import com.example.customlauncher.core.model.App.UserApp
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    fun getAppsStream(): Flow<List<UserApp?>>

    suspend fun refreshApps()

    suspend fun editAppName(newName: String, app: UserApp)

    suspend fun handleNotis(notifications: List<StatusBarNotification>)

    suspend fun moveApp(packageName: String, toIndex: Int)
}