package com.example.customlauncher.core.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.customlauncher.core.common.util.mergeWith
import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.data.util.asEntity
import com.example.customlauncher.core.data.util.packageName
import com.example.customlauncher.core.data.util.resolveInfoMap
import com.example.customlauncher.core.database.AppDao
import com.example.customlauncher.core.database.model.AppEntity
import com.example.customlauncher.core.database.model.asExternalModel
import com.example.customlauncher.core.database.model.canUninstall
import com.example.customlauncher.core.database.model.isInstalledAndUpToDate
import com.example.customlauncher.core.designsystem.util.asBitmap
import com.example.customlauncher.core.model.App
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

class OfflineFirstAppRepository @Inject constructor(
    private val appDao: AppDao,
    @ApplicationContext private val context: Context,
) : AppRepository {

    private val pm = context.packageManager
    private val resolveInfo get() = pm.resolveInfoMap

    private val usageStatsManager = context
        .getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private enum class AppType { User, Company }

    override fun getAppsStream(): Flow<List<List<App>>> {
        return appDao.observeAll().map { apps ->

            val pagedApps = apps.groupBy { app -> app.page }.mapValues { entityList ->
                entityList.value.mapNotNull { entity ->
                    entity.asExternalModel(
                        icon = resolveInfo[entity.packageName]?.loadIcon(pm)?.asBitmap(),
                        canUninstall = entity.canUninstall(pm)
                    )
                }
            }

            return@map pagedApps
                .toSortedMap()
                .mapValues { it.value.sortedBy { app -> app.index } }
                .values.toList()
        }
    }

    private val refreshApplicationMutex = Mutex()
    override suspend fun refreshApps() {
        refreshApplicationMutex.lock()

        val resolveInfo = resolveInfo
        appDao.deleteUninstalled(resolveInfo.keys.toList())

        val userDbApps = appDao.getAll()

        val pageMap = getPackageNameAppTypeMapByPage(userDbApps, companyDbApps)
            .toSortedMap()
            .toMutableMap()

        val userDbAppMap = userDbApps.associateBy { it.packageName }
        val isFirstPageAllCompanyApps = pageMap[0]?.all { it.second == AppType.Company } ?: false

        val currentUserApps = resolveInfo.values.map { info ->
            userDbAppMap[info.packageName]?.let {
                return@map info.asEntity(
                    packageManager = pm,
                    index = it.index,
                    page = it.page
                )
            }

            val page = calculatePage(pageMap, gridCount, isFirstPageAllCompanyApps)
            info.asEntity(
                packageManager = pm,
                page = page,
                index = calculateIndexAndUpdateTempMap(
                    pageMap,
                    page,
                    info.packageName,
                    AppType.User
                ),
            )
        }

        for (app in currentUserApps) {
            if (!app.isInstalledAndUpToDate(userDbAppMap)) {
                appDao.upsert(app)
            } else {
                Log.d("DB Client", "UserApp ${app.packageName} is up to date")
            }
        }

        usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, 0, System.currentTimeMillis()
        ).forEach {
            appDao.updateUsageTime(it.totalTimeInForeground, it.packageName)
        }

        refreshApplicationMutex.unlock()
    }

    override suspend fun editAppName(newName: String, app: App) {
        appDao.updateName(newName, app.packageName)
    }

    private val handleNotificationsMutex = Mutex()
    override suspend fun handleNotifications(notifications: List<StatusBarNotification>) {
        handleNotificationsMutex.lock()
        appDao.unsetAllNotificationCount()
        notifications.groupingBy { it.packageName }.eachCount().forEach {
            appDao.updateNotificationCount(it.value, it.key)
        }
        handleNotificationsMutex.unlock()
    }

    override suspend fun moveInPage(toIndex: Int, app: App) {
        appDao.updateIndexByPackageName(toIndex, app.packageName)
    }

    override suspend fun moveToPage(toPage: Int, apps: List<App>) {
        val userDbApps = appDao.getAll()
        val pageMap = getPackageNameAppTypeMapByPage(userDbApps, companyDbApps)
        val remainingPageSpace = gridCount - (pageMap[toPage]?.size ?: 0)
        var latestIndex = pageMap[toPage]?.lastIndex ?: 0
        apps.take(remainingPageSpace).forEach { app ->
            appDao.updatePageAndIndexByPackageName(
                    toPage, ++latestIndex,
                    app.packageName
            )
        }
    }

    private fun getPackageNameAppTypeMapByPage(
        userDbApps: List<AppEntity>,
    ): Map<Int, List<Pair<String, AppType>>> {
        val userAppPageMap = userDbApps.groupBy { app -> app.page }
            .mapValues {
                it.value.sortedBy { app -> app.index }
                    .map { app -> Pair(app.packageName, AppType.User) }
            }

        return userAppPageMap.mergeWith(companyAppPageMap)
    }

    private fun calculatePage(
        packageNameMap: MutableMap<Int, List<Pair<String, AppType>>>,
        gridCount: Int,
        isFirstPageAllCompanyApps: Boolean
    ): Int {
        if (packageNameMap.isEmpty()) {
            return 0
        }

        for (i in packageNameMap) {
            if (isFirstPageAllCompanyApps && i.key == 0) continue
            val count = i.value.size
            if (gridCount > count) {
                return i.key
            }
        }

        return packageNameMap.keys.max() + 1
    }

    private fun calculateIndexAndUpdateTempMap(
        packageNameMap: MutableMap<Int, List<Pair<String, AppType>>>,
        page: Int,
        packageName: String,
        appType: AppType
    ): Int {
        if (packageNameMap[page].isNullOrEmpty()) {
            packageNameMap[page] = listOf(Pair(packageName, appType))
            return 0
        }

        packageNameMap.compute(page) { _, value ->
            (value?.toMutableList() ?: mutableListOf()).apply { add(Pair(packageName, appType)) }
        }

        return packageNameMap[page]?.lastIndex ?: 0
    }
}

