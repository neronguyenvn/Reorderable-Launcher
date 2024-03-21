package com.example.customlauncher.core.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.data.util.asApplicationEntity
import com.example.customlauncher.core.data.util.packageName
import com.example.customlauncher.core.database.ApplicationDao
import com.example.customlauncher.core.database.model.PageCount
import com.example.customlauncher.core.database.model.asUserApp
import com.example.customlauncher.core.database.model.canUninstall
import com.example.customlauncher.core.database.model.isInstalledAndUpToDate
import com.example.customlauncher.core.designsystem.util.asBitmap
import com.example.customlauncher.core.model.App.UserApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

class OfflineFirstAppRepository @Inject constructor(
    private val appDao: ApplicationDao,

    @ApplicationContext
    private val context: Context,
) : AppRepository {

    private val pm = context.packageManager

    private val resolveInfo
        get() = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            PackageManager.GET_META_DATA
        ).associateBy { it.packageName }

    private val usageStatsManager = context
        .getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val _gridCount = MutableStateFlow(0)

    override fun getAppsStream(): Flow<Map<Int, List<UserApp>>> {
        return appDao.observeAll().map { list ->
            list.groupBy { app -> app.page }.mapValues { entityList ->
                entityList.value.sortedBy { it.index }.mapNotNull { entity ->
                    entity.asUserApp(
                        icon = resolveInfo[entity.packageName]?.loadIcon(pm)?.asBitmap(),
                        canUninstall = entity.canUninstall(pm)
                    )
                }
            }
        }
    }

    private val refreshApplicationMutex = Mutex()
    override suspend fun refreshApps() {
        refreshApplicationMutex.lock()

        val dbApps = appDao.observeAll().first().associateBy { it.packageName }
        var latestIndex = appDao.getLatestIndex()

        val gridCount = _gridCount.first { it != 0 }
        val pageCounts = appDao.getPageCounts().toMutableList()

        val currentApps = resolveInfo.values.mapIndexed { index, info ->
            info.asApplicationEntity(
                packageManager = pm,
                index = dbApps[info.packageName]?.index ?: ++latestIndex,
                page = dbApps[info.packageName]?.page ?: if (pageCounts.isEmpty()) {
                    index / gridCount
                } else calculatePage(gridCount, pageCounts)
            )
        }

        for (app in currentApps) {
            if (!app.isInstalledAndUpToDate(appDao)) {
                appDao.upsert(app)
            } else {
                Log.d("neronguyenvn", "UserApp ${app.packageName} is up to date")
            }
        }
        appDao.deleteUninstalledUserApp(currentApps.map { it.packageName })

        usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_WEEKLY,
            0,
            System.currentTimeMillis()
        ).forEach {
            appDao.updateUsageTime(it.totalTimeInForeground, it.packageName)
        }

        refreshApplicationMutex.unlock()
    }

    override suspend fun editAppName(newName: String, app: UserApp) {
        appDao.updateName(newName, app.packageName)
    }

    private val handleNotificationsMutex = Mutex()
    override suspend fun handleNotis(notifications: List<StatusBarNotification>) {
        handleNotificationsMutex.lock()
        appDao.unsetAllNotificationCount()
        notifications.groupingBy { it.packageName }.eachCount().forEach {
            appDao.updateNotificationCount(it.value, it.key)
        }
        handleNotificationsMutex.unlock()
    }

    override suspend fun moveApp(packageName: String, toIndex: Int) {
        appDao.updateIndexByPackageName(packageName, toIndex)
    }

    override fun updateGridCount(value: Int) {
        _gridCount.value = value
    }

    private fun calculatePage(gridCount: Int, pageCounts: MutableList<PageCount>): Int {
        pageCounts.forEachIndexed { index, page ->
            if (gridCount > page.count) {
                pageCounts[index] = page.copy(count = page.count + 1)
                return page.pageIndex
            }
        }
        return pageCounts.last().pageIndex + 1
    }
}

