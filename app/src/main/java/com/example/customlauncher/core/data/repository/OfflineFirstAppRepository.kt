package com.example.customlauncher.core.data.repository

import android.content.Context
import android.util.Log
import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.data.util.appInfoMap
import com.example.customlauncher.core.data.util.asEntity
import com.example.customlauncher.core.data.util.packageName
import com.example.customlauncher.core.database.AppDao
import com.example.customlauncher.core.database.model.AppEntity
import com.example.customlauncher.core.database.model.asExternalModel
import com.example.customlauncher.core.database.model.canUninstall
import com.example.customlauncher.core.database.model.isInstalledAndUpToDate
import com.example.customlauncher.core.designsystem.util.asBitmap
import com.example.customlauncher.core.model.App
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

private const val TAG = "OfflineFirstAppRepository"

class OfflineFirstAppRepository @Inject constructor(
    private val appDao: AppDao,
    @ApplicationContext private val context: Context,
) : AppRepository {

    private val pm = context.packageManager
    private val maxAppsPerPage = MutableSharedFlow<Int>(replay = 1)

    override fun getAppsStream(): Flow<List<List<App>>> {

        val appInfoMap = pm.appInfoMap
        return appDao.observeAll().map { apps ->

            val pagedApps = apps.groupBy { it.page }.mapValues { entry ->
                entry.value.mapNotNull { entity ->
                    entity.asExternalModel(
                        icon = appInfoMap[entity.packageName]?.loadIcon(pm)?.asBitmap(),
                        canUninstall = entity.canUninstall(pm)
                    )
                }
            }

            val sortedPagedApps = pagedApps
                .toSortedMap()
                .values
                .map { it.sortedBy { app -> app.index } }

            return@map sortedPagedApps
        }
    }

    private val refreshApplicationMutex = Mutex()
    override suspend fun refreshApps() = refreshApplicationMutex.withLock {
        val dbApps = appDao.getAll()
        val dbAppMap = dbApps.associateBy { it.packageName }
        val tempMap = getAppTempMap(dbApps)

        val appInfoList = pm.appInfoMap.values
        appDao.deleteUninstalled(appInfoList.map { it.packageName })

        for (appInfo in appInfoList) {
            val existingApp = dbAppMap[appInfo.packageName]
            if (existingApp != null && existingApp.isInstalledAndUpToDate(dbAppMap)) {
                Log.d(TAG, "${appInfo.packageName} is up to date")
                continue
            }

            val page = calculatePage(tempMap, maxAppsPerPage.first())
            val newApp = appInfo.asEntity(
                packageManager = pm,
                page = page,
                index = calculateIndexAndUpdateTempMap(tempMap, appInfo.packageName, page),
            )

            appDao.upsert(newApp)
        }
    }

    override suspend fun editAppName(newName: String, app: App) {
        appDao.updateName(newName, app.packageName)
    }

    override suspend fun moveInPage(toIndex: Int, app: App) {
        appDao.updateIndexByPackageName(toIndex, app.packageName)
    }

    override suspend fun moveToPage(toPage: Int, apps: List<App>) {
        val tempMap = getAppTempMap(appDao.getAll())
        val remainingPageSpace = maxAppsPerPage.first() - (tempMap[toPage]?.size ?: 0)
        var latestIndex = tempMap[toPage]?.lastIndex ?: 0
        apps.take(remainingPageSpace).forEach { app ->
            appDao.updatePageAndIndexByPackageName(
                toPage, ++latestIndex,
                app.packageName
            )
        }
    }

    override suspend fun updateMaxAppsPerPage(count: Int) {
        maxAppsPerPage.emit(count)
    }

    private fun getAppTempMap(
        dbApps: List<AppEntity>,
    ): MutableMap<Int, MutableList<String>> {
        val newMap = dbApps.groupBy { it.page }
            .mapValues { entry ->
                entry.value.sortedBy { it.index }.map { it.packageName }.toMutableList()
            }
            .toSortedMap()
            .toMutableMap()

        return if (newMap.isEmpty()) {
            mutableMapOf(0 to mutableListOf())
        } else newMap
    }

    private fun calculatePage(
        pagedApps: MutableMap<Int, MutableList<String>>,
        maxAppsPerPage: Int,
    ): Int {
        if (pagedApps.isEmpty()) {
            return 0
        }

        for (page in pagedApps) {
            val count = page.value.size
            if (maxAppsPerPage > count) {
                return page.key
            }
        }

        return pagedApps.keys.last() + 1
    }

    private fun calculateIndexAndUpdateTempMap(
        tempMap: MutableMap<Int, MutableList<String>>,
        packageName: String,
        page: Int,
    ): Int {
        if (tempMap.containsKey(page)) {
            tempMap[page]?.add(packageName)
        } else {
            tempMap[page] = mutableListOf(packageName)
        }
        return tempMap[page]!!.lastIndex
    }
}
