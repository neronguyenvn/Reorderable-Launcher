package com.example.customlauncher.core.data.implementation

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.customlauncher.core.data.ApplicationRepository
import com.example.customlauncher.core.data.util.asApplicationEntity
import com.example.customlauncher.core.data.util.packageName
import com.example.customlauncher.core.database.ApplicationDao
import com.example.customlauncher.core.database.model.asUserApp
import com.example.customlauncher.core.database.model.canUninstall
import com.example.customlauncher.core.database.model.isInstalledAndUpToDate
import com.example.customlauncher.core.designsystem.util.asBitmap
import com.example.customlauncher.core.model.Application
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject


class OfflineFirstApplicationRepository @Inject constructor(
    private val appDao: ApplicationDao,

    @ApplicationContext
    private val context: Context
) : ApplicationRepository {

    private val pm = context.packageManager

    private val resolveInfo
        get() = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            PackageManager.GET_META_DATA
        ).associateBy { it.packageName }

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager


    override fun getApplicationsStream(): Flow<List<Application?>> {
        return appDao.observeAll().map { list ->
            list.sortedBy { it.index }.map { entity ->
                entity.asUserApp(
                    icon = resolveInfo[entity.packageName]?.loadIcon(pm)?.asBitmap(),
                    canUninstall = entity.canUninstall(pm)
                )
            }
        }
    }

    private val refreshApplicationMutex = Mutex()
    override suspend fun refreshApplications() {
        refreshApplicationMutex.lock()
        val apps =
            resolveInfo.values.mapIndexed { index, info -> info.asApplicationEntity(pm, index) }
        for (app in apps) {
            if (!app.isInstalledAndUpToDate(appDao)) {
                appDao.upsert(app)
            } else {
                Log.d("NERO", "UserApp ${app.packageName} is up to date")
            }
        }
        appDao.deleteUninstalledUserApp(apps.map { it.packageName })

        usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_WEEKLY,
            0,
            System.currentTimeMillis()
        ).forEach {
            appDao.updateUsageTime(it.totalTimeInForeground, it.packageName)
        }

        refreshApplicationMutex.unlock()
    }

    override suspend fun editName(name: String, userApp: Application.UserApp) {
        appDao.updateName(name, userApp.packageName)
    }

    private val handleNotificationsMutex = Mutex()
    override suspend fun handleNotifications(notifications: List<StatusBarNotification>) {
        handleNotificationsMutex.lock()
        appDao.unsetAllNotificationCount()
        notifications.groupingBy { it.packageName }.eachCount()
            .forEach { appDao.updateNotificationCount(it.value, it.key) }
        handleNotificationsMutex.unlock()
    }

    override suspend fun moveApplication(packageName: String, toIndex: Int) {
        appDao.updateIndexByPackageName(packageName, toIndex)
    }
}

