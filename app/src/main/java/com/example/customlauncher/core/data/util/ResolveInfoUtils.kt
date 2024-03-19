package com.example.customlauncher.core.data.util

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.example.customlauncher.core.database.model.UserAppEntity

val ResolveInfo.packageName: String get() = activityInfo.packageName

fun ResolveInfo.asApplicationEntity(pm: PackageManager, index: Int) = UserAppEntity(
    name = loadLabel(pm).toString(),
    packageName = packageName,
    version = pm.getPackageInfo(packageName, 0).versionName,
    index = index
)
