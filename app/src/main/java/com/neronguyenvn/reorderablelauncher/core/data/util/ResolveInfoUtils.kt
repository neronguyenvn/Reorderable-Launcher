package com.neronguyenvn.reorderablelauncher.core.data.util

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.neronguyenvn.reorderablelauncher.core.database.model.AppEntity

val PackageManager.appInfoMap: Map<String, ResolveInfo>
    get() = this.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        PackageManager.GET_META_DATA
    ).associateBy { it.packageName }

val ResolveInfo.packageName: String get() = activityInfo.packageName

fun ResolveInfo.asEntity(
    packageManager: PackageManager,
    index: Int,
    page: Int
) = AppEntity(
    name = loadLabel(packageManager).toString(),
    packageName = packageName,
    version = packageManager.getPackageInfo(packageName, 0).versionName ?: "",
    index = index,
    page = page
)
