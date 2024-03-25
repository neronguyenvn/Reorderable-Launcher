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
import com.example.customlauncher.core.database.CompanyAppDao
import com.example.customlauncher.core.database.UserAppDao
import com.example.customlauncher.core.database.model.CompanyAppEntity
import com.example.customlauncher.core.database.model.UserAppEntity
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
    private val resolveInfo get() = pm.resolveInfoMap

    private val usageStatsManager = context
        .getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val _gridCount = MutableStateFlow(0)

    private enum class AppType { User, Company }

    override fun getAppsStream(): Flow<List<List<App>>> {
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
                .toSortedMap()
                .mapValues { it.value.sortedBy { app -> app.index } }
                .values.toList()
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

        val pageMap = getPackageNameAppTypeMapByPage(userDbApps, companyDbApps)
            .toSortedMap()
            .toMutableMap()

        val userDbAppMap = userDbApps.associateBy { it.packageName }
        val gridCount = _gridCount.first { it != 0 }
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

    override suspend fun refreshCompanyApps() {
        val networkCompanyApps = network.getCompanyApps().apps
        companyAppDao.deleteUninstalled(networkCompanyApps.map { it.packageName })

        val userDbApps = userAppDao.getAll()
        val companyDbApps = companyAppDao.getAll()

        val pageMap = getPackageNameAppTypeMapByPage(userDbApps, companyDbApps)
            .toSortedMap()
            .toMutableMap()

        val companyDbAppMap = companyDbApps.associateBy { it.packageName }
        val gridCount = _gridCount.first { it != 0 }

        val currentCompanyApps = networkCompanyApps.map { app ->
            companyDbAppMap[app.packageName]?.let { entity ->
                return@map app.asEntity(
                    index = entity.index,
                    page = entity.page,
                )
            }

            val page = calculatePage(pageMap, gridCount, false)
            app.asEntity(
                page = page,
                index = calculateIndexAndUpdateTempMap(
                    pageMap,
                    page,
                    app.packageName,
                    AppType.Company
                ),
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
            is CompanyApp -> companyAppDao.updateIndexByPackageName(toIndex, app.packageName)
        }
    }

    override suspend fun moveToPage(toPage: Int, apps: List<App>) {
        val userDbApps = userAppDao.getAll()
        val companyDbApps = companyAppDao.getAll()
        val pageMap = getPackageNameAppTypeMapByPage(userDbApps, companyDbApps)
        val gridCount = _gridCount.first { it != 0 }
        val remainingPageSpace = gridCount - (pageMap[toPage]?.size ?: 0)
        var latestIndex = pageMap[toPage]?.lastIndex ?: 0
        apps.take(remainingPageSpace).forEach { app ->
            when (app) {
                is UserApp -> userAppDao.updatePageAndIndexByPackageName(
                    toPage, ++latestIndex,
                    app.packageName
                )

                is CompanyApp -> companyAppDao.updatePageAndIndexByPackageName(
                    toPage, ++latestIndex,
                    app.packageName
                )
            }
        }
    }

    private fun getPackageNameAppTypeMapByPage(
        userDbApps: List<UserAppEntity>,
        companyDbApps: List<CompanyAppEntity>
    ): Map<Int, List<Pair<String, AppType>>> {
        val userAppPageMap = userDbApps.groupBy { app -> app.page }
            .mapValues {
                it.value.sortedBy { app -> app.index }
                    .map { app -> Pair(app.packageName, AppType.User) }
            }

        val companyAppPageMap = companyDbApps.groupBy { it.page }
            .mapValues {
                it.value.sortedBy { app -> app.index }
                    .map { app -> Pair(app.packageName, AppType.Company) }
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

