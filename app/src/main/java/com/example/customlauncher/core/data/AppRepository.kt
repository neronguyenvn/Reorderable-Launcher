package com.example.customlauncher.core.data

import android.service.notification.StatusBarNotification
import com.example.customlauncher.core.model.App.CompanyApp
import com.example.customlauncher.core.model.App.UserApp
import kotlinx.coroutines.flow.Flow

interface AppRepository {

    fun getUserAppsStream(): Flow<Map<Int, List<UserApp>>>

    fun getCompanyAppsStream(): Flow<List<CompanyApp>>

    fun updateGridCount(value: Int)

    suspend fun refreshApps()

    suspend fun editAppName(newName: String, app: UserApp)

    suspend fun handleNotis(notifications: List<StatusBarNotification>)

    suspend fun moveUserApp(toIndex: Int, app: UserApp)

    suspend fun moveCompanyApp(toIndex: Int, app: CompanyApp)
}