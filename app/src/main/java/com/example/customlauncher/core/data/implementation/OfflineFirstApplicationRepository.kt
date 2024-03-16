package com.example.customlauncher.core.data.implementation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.example.customlauncher.core.data.ApplicationRepository
import com.example.customlauncher.core.data.util.asApplicationEntity
import com.example.customlauncher.core.data.util.packageName
import com.example.customlauncher.core.database.ApplicationDao
import com.example.customlauncher.core.database.model.asUserApp
import com.example.customlauncher.core.database.model.isInstalledAndUpToDate
import com.example.customlauncher.core.model.Application
import com.example.customlauncher.core.util.asBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

class OfflineFirstApplicationRepository @Inject constructor(
    private val appDao: ApplicationDao,

    @ApplicationContext
    private val context: Context,
) : ApplicationRepository {

    private val pm = context.packageManager
    private val resolveInfo
        get() = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            PackageManager.GET_META_DATA
        ).associateBy { it.packageName }


    override fun getApplicationsStream(): Flow<List<Application?>> {
        return appDao.observeAll().map { list ->
            list.map { entity ->
                entity.asUserApp(resolveInfo[entity.packageName]?.loadIcon(pm)?.asBitmap())
            }
        }
    }

    private val refreshApplicationMutex = Mutex()

    override suspend fun refreshApplications() {
        refreshApplicationMutex.lock()
        val apps = resolveInfo.values.map { it.asApplicationEntity(pm) }
        for (app in apps) {
            if (!app.isInstalledAndUpToDate(appDao)) {
                appDao.upsert(app)
            } else {
                Log.d("NERO", "UserApp ${app.packageName} is up to date")
            }
        }
        appDao.deleteUninstalledUserApp(apps.map { it.packageName })
        refreshApplicationMutex.unlock()
    }
}

