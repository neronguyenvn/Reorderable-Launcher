package com.example.customlauncher.core.data

import android.content.pm.ResolveInfo
import com.example.customlauncher.core.model.Application
import kotlinx.coroutines.flow.Flow

interface ApplicationRepository {

    fun getApplicationsStream(): Flow<List<Application?>>

    suspend fun refreshApplications()
}