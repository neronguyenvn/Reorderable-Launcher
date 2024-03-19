package com.example.customlauncher.core.data

import android.service.notification.StatusBarNotification
import com.example.customlauncher.core.model.Application
import kotlinx.coroutines.flow.Flow

interface ApplicationRepository {

    fun getApplicationsStream(): Flow<List<Application?>>

    suspend fun refreshApplications()

    suspend fun editName(name: String, userApp: Application.UserApp)

    suspend fun handleNotifications(notifications: List<StatusBarNotification>)

    suspend fun moveApplication(packageName: String, toIndex: Int)
}