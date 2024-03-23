package com.example.customlauncher.core.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.customlauncher.core.common.util.mergeWith
import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.data.util.asEntity
import com.example.customlauncher.core.data.util.packageName
import com.example.customlauncher.core.database.CompanyAppDao
import com.example.customlauncher.core.database.UserAppDao
import com.example.customlauncher.core.database.model.asExternalModel
import com.example.customlauncher.core.database.model.canUninstall
import com.example.customlauncher.core.database.model.isInstalledAndUpToDate
import com.example.customlauncher.core.designsystem.util.asBitmap
import com.example.customlauncher.core.model.App
import com.example.customlauncher.core.model.App.CompanyApp
import com.example.customlauncher.core.model.App.UserApp
import com.example.customlauncher.core.network.ClNetworkDataSource
import com.example.customlauncher.core.network.model.asEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

class OfflineFirstAppRepository @Inject constructor(
    private val network: ClNetworkDataSource,
    private val userAppDao: UserAppDao,
    private val companyAppDao: CompanyAppDao,

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

    override fun getAppsStream(): Flow<Map<Int, List<App>>> {
        return userAppDao.observeAll().combine(companyAppDao.observeAll()) { users, companies ->

            val pagedUsers = users.groupBy { app -> app.page }.mapValues { entityList ->
                entityList.value.mapNotNull { entity ->
                    entity.asExternalModel(
                        icon = resolveInfo[entity.packageName]?.loadIcon(pm)?.asBitmap(),
                        canUninstall = entity.canUninstall(pm)
                    )
                }
            }

            val pagedCompanies = companies.groupBy { app -> app.page }.mapValues { entityList ->
                entityList.value.map { entity ->
                    entity.asExternalModel()
                }
            }

            return@combine pagedUsers.mergeWith(pagedCompanies)
                .mapValues { it.value.sortedBy { app -> app.index } }
        }
    }

    override fun updateGridCount(value: Int) {
        _gridCount.value = value
    }

    private val refreshApplicationMutex = Mutex()
    override suspend fun refreshUserApps() {
        refreshApplicationMutex.lock()

        val resolveInfo = resolveInfo
        userAppDao.deleteUninstalled(resolveInfo.keys.toList())

        val userDbApps = userAppDao.getAll()
        val companyDbApps = companyAppDao.getAll()

        val userAppPageMap = userDbApps.groupBy { app -> app.page }
            .mapValues {
                it.value.sortedBy { app -> app.index }
                    .map { app -> app.packageName }
            }

        val companyAppPageMap = companyDbApps.groupBy { it.page }
            .mapValues {
                it.value.sortedBy { app -> app.index }
                    .map { app -> app.packageName }
            }

        val pageMap = userAppPageMap.mergeWith(companyAppPageMap).toSortedMap().toMutableMap()
        val userDbAppMap = userDbApps.associateBy { it.packageName }

        val gridCount = _gridCount.first { it != 0 }

        val currentUserApps = resolveInfo.values.map { info ->
            userDbAppMap[info.packageName]?.let {
                return@map info.asEntity(
                    packageManager = pm,
                    index = it.index,
                    page = it.page
                )
            }

            val page = calculatePage(pageMap, gridCount)
            info.asEntity(
                packageManager = pm,
                index = calculateIndexAndUpdateTempMap(pageMap, page, info.packageName),
                page = page
            )
        }

        for (app in currentUserApps) {
            if (!app.isInstalledAndUpToDate(userDbAppMap)) {
                userAppDao.upsert(app)
            } else {
                Log.d("DB Client", "UserApp ${app.packageName} is up to date")
            }
        }

        usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, 0, System.currentTimeMillis()
        ).forEach {
            userAppDao.updateUsageTime(it.totalTimeInForeground, it.packageName)
        }

        refreshApplicationMutex.unlock()
    }


    // TODO: Need to check version to upgrade existing apps
    override suspend fun refreshCompanyApps() {
        val networkCompanyApps = network.getCompanyApps().apps
        userAppDao.deleteUninstalled(networkCompanyApps.map { it.packageName })

        val userDbApps = userAppDao.getAll()
        val companyDbApps = companyAppDao.getAll()

        val userAppPageMap = userDbApps.groupBy { app -> app.page }
            .mapValues {
                it.value.sortedBy { app -> app.index }
                    .map { app -> app.packageName }
            }

        val companyAppPageMap = companyDbApps.groupBy { it.page }
            .mapValues {
                it.value.sortedBy { app -> app.index }
                    .map { app -> app.packageName }
            }

        val pageMap = userAppPageMap.mergeWith(companyAppPageMap).toSortedMap().toMutableMap()
        val companyDbAppMap = companyDbApps.associateBy { it.packageName }

        val gridCount = _gridCount.first { it != 0 }

        val currentCompanyApps = networkCompanyApps.map { app ->
            companyDbAppMap[app.packageName]?.let { entity ->
                return@map app.asEntity(
                    index = entity.index,
                    page = entity.page,
                )
            }

            val page = calculatePage(pageMap, gridCount)
            app.asEntity(
                index = calculateIndexAndUpdateTempMap(pageMap, page, app.packageName),
                page = page
            )
        }

        for (app in currentCompanyApps) {
            if (!app.isInstalledAndUpToDate(companyDbAppMap)) {
                companyAppDao.upsert(app)
            } else {
                Log.d("DB Client", "Company ${app.packageName} is up to date")
            }
        }
    }

    override suspend fun editAppName(newName: String, app: UserApp) {
        userAppDao.updateName(newName, app.packageName)
    }

    private val handleNotificationsMutex = Mutex()
    override suspend fun handleNotis(notifications: List<StatusBarNotification>) {
        handleNotificationsMutex.lock()
        userAppDao.unsetAllNotificationCount()
        notifications.groupingBy { it.packageName }.eachCount().forEach {
            userAppDao.updateNotificationCount(it.value, it.key)
        }
        handleNotificationsMutex.unlock()
    }

    override suspend fun moveInPage(toIndex: Int, app: App) {
        when (app) {
            is UserApp -> userAppDao.updateIndexByPackageName(toIndex, app.packageName)
            is CompanyApp -> companyAppDao.updateIndexById(toIndex, app.packageName)
        }
    }

    private fun calculatePage(
        map: MutableMap<Int, List<String>>,
        gridCount: Int,
    ): Int {
        if (map.isEmpty()) {
            return 0
        }

        map.forEach {
            val count = it.value.size
            if (gridCount > count) {
                return it.key
            }
        }

        return map.keys.max() + 1
    }

    private fun calculateIndexAndUpdateTempMap(
        map: MutableMap<Int, List<String>>,
        page: Int,
        packageName: String,
    ): Int {
        if (map[page].isNullOrEmpty()) {
            map[page] = listOf(packageName)
            return 0
        }

        map.compute(page) { _, value ->
            value!!.toMutableList().apply { add(packageName) }
        }

        return map[page]!!.lastIndex
    }
}

