package com.example.customlauncher.core.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.customlauncher.core.common.coroutine.ClDispatcher.IO
import com.example.customlauncher.core.common.coroutine.Dispatcher
import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.data.util.asApplicationEntity
import com.example.customlauncher.core.data.util.packageName
import com.example.customlauncher.core.database.CompanyAppDao
import com.example.customlauncher.core.database.UserAppDao
import com.example.customlauncher.core.database.model.PageCount
import com.example.customlauncher.core.database.model.asExternalModel
import com.example.customlauncher.core.database.model.canUninstall
import com.example.customlauncher.core.database.model.isInstalledAndUpToDate
import com.example.customlauncher.core.designsystem.util.asBitmap
import com.example.customlauncher.core.model.App.CompanyApp
import com.example.customlauncher.core.model.App.UserApp
import com.example.customlauncher.core.network.ClNetworkDataSource
import com.example.customlauncher.core.network.model.asEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import javax.inject.Inject

class OfflineFirstAppRepository @Inject constructor(
    private val network: ClNetworkDataSource,
    private val userAppDao: UserAppDao,
    private val companyAppDao: CompanyAppDao,

    @ApplicationContext
    private val context: Context,

    @Dispatcher(IO)
    private val ioDispatcher: CoroutineDispatcher
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

    override fun getUserAppsStream(): Flow<Map<Int, List<UserApp>>> {
        return userAppDao.observeAll().map { list ->
            list.groupBy { app -> app.page }.mapValues { entityList ->
                entityList.value.sortedBy { it.index }.mapNotNull { entity ->
                    entity.asExternalModel(
                        icon = resolveInfo[entity.packageName]?.loadIcon(pm)?.asBitmap(),
                        canUninstall = entity.canUninstall(pm)
                    )
                }
            }
        }
    }

    override fun getCompanyAppsStream(): Flow<List<CompanyApp>> {
        return companyAppDao.observeAll().map { list ->
            list.sortedBy { it.index }.map { entity ->
                entity.asExternalModel()
            }
        }
    }

    override fun updateGridCount(value: Int) {
        _gridCount.value = value
    }

    private val refreshApplicationMutex = Mutex()
    override suspend fun refreshApps() {
        refreshApplicationMutex.lock()

        withContext(ioDispatcher) {
            launch {
                val companyApps = async { network.getCompanyApps() }

                val dbApps = companyAppDao.observeAll().first().associateBy { it.id }
                var latestIndex = companyAppDao.getLatestIndex()

                companyApps.await().apps.forEach {
                    companyAppDao.upsert(
                        it.asEntity(
                            index = dbApps[it.id]?.index ?: ++latestIndex,
                            page = 0,
                            isFavorite = false
                        )
                    )
                }
                companyApps.await().favorites.forEach {
                    companyAppDao.upsert(
                        it.asEntity(
                            index = dbApps[it.id]?.index ?: ++latestIndex,
                            page = 0,
                            isFavorite = false
                        )
                    )
                }
            }
        }

        val dbApps = userAppDao.observeAll().first().associateBy { it.packageName }
        var latestIndex = userAppDao.getLatestIndex()

        val gridCount = _gridCount.first { it != 0 }
        val pageCounts = userAppDao.getPageCounts().toMutableList()

        val currentApps = resolveInfo.values.mapIndexed { index, info ->
            info.asApplicationEntity(
                packageManager = pm,
                index = dbApps[info.packageName]?.index ?: ++latestIndex,
                page = dbApps[info.packageName]?.page ?: if (pageCounts.isEmpty()) {
                    index / gridCount + 1
                } else calculatePage(gridCount, pageCounts)
            )
        }

        for (app in currentApps) {
            if (!app.isInstalledAndUpToDate(userAppDao)) {
                userAppDao.upsert(app)
            } else {
                Log.d("neronguyenvn", "UserApp ${app.packageName} is up to date")
            }
        }
        userAppDao.deleteUninstalledUserApp(currentApps.map { it.packageName })

        usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_WEEKLY,
            0,
            System.currentTimeMillis()
        ).forEach {
            userAppDao.updateUsageTime(it.totalTimeInForeground, it.packageName)
        }

        refreshApplicationMutex.unlock()
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

    override suspend fun moveUserApp(toIndex: Int, app: UserApp) {
        userAppDao.updateIndexByPackageName(toIndex, app.packageName)
    }

    override suspend fun moveCompanyApp(toIndex: Int, app: CompanyApp) {
        companyAppDao.updateIndexById(toIndex, app.packageName)
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

