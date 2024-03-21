package com.example.customlauncher.core.data

import android.service.notification.StatusBarNotification
import com.example.customlauncher.core.model.App.CompanyApp
import com.example.customlauncher.core.model.App.UserApp
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    fun getUserAppsStream(): Flow<Map<Int, List<UserApp>>>

    fun getCompanyAppsStream(): Flow<List<CompanyApp>>

    suspend fun refreshApps()

    suspend fun editAppName(newName: String, app: UserApp)

    suspend fun handleNotis(notifications: List<StatusBarNotification>)

    suspend fun moveApp(packageName: String, toIndex: Int)

    fun updateGridCount(value: Int)
}