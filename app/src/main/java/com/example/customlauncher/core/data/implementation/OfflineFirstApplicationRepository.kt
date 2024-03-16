package com.example.customlauncher.core.data.implementation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.ui.input.key.Key.Companion.U
import com.example.customlauncher.core.common.coroutine.ClDispatcher.IO
import com.example.customlauncher.core.common.coroutine.Dispatcher
import com.example.customlauncher.core.common.coroutine.di.ApplicationScope
import com.example.customlauncher.core.data.ApplicationRepository
import com.example.customlauncher.core.database.ApplicationDao
import com.example.customlauncher.core.database.model.ApplicationEntity
import com.example.customlauncher.core.database.model.ApplicationType.*
import com.example.customlauncher.core.database.model.asCompanyApp
import com.example.customlauncher.core.database.model.asUserApp
import com.example.customlauncher.core.model.Application
import com.example.customlauncher.core.util.asBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMap
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class OfflineFirstApplicationRepository @Inject constructor(
    private val applicationDao: ApplicationDao,

    @ApplicationContext
    private val context: Context,

    @Dispatcher(IO)
    private val ioDispatcher: CoroutineDispatcher,

    @ApplicationScope
    private val applicationScope: CoroutineScope,
) : ApplicationRepository {

    private val packageManager = context.packageManager
    private val resolveInfo = packageManager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        },
        PackageManager.MATCH_ALL
    )


    override fun getApplicationsStream(): Flow<List<Application?>> {

        val drawableMap = resolveInfo.associate {
            it.getPackageName() to it.loadIcon(packageManager)
        }

        return applicationDao.observeAll().map { list ->
            list.map { entity ->
                when (entity.type) {
                    COMPANY -> entity.asCompanyApp()
                    USER -> {
                        drawableMap[entity.packageName]?.asBitmap()?.let {
                            entity.asUserApp(it)
                        } ?: let {
                            applicationScope.launch {
                                applicationDao.delete(entity)
                            }
                            null
                        }
                    }
                }
            }
        }
    }

    override suspend fun refreshApplications() {
        resolveInfo.map {
            val packageName = it.activityInfo.packageName
            applicationDao.upsert(
                ApplicationEntity(
                    name = it.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    version = packageManager.getPackageInfo(packageName, 0).versionName,
                    type = USER
                )
            )
        }
    }
}

fun ResolveInfo.getPackageName(): String = activityInfo.packageName