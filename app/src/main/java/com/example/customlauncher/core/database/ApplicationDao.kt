package com.example.customlauncher.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.customlauncher.core.database.model.UserAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {

    @Query("SELECT * FROM UserApp")
    fun observeAll(): Flow<List<UserAppEntity>>

    @Upsert
    suspend fun upsert(applicationEntity: UserAppEntity)

    @Query("SELECT * FROM UserApp WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): UserAppEntity?

    @Query("DELETE FROM UserApp WHERE packageName NOT IN (:packages)")
    suspend fun deleteUninstalledUserApp(packages: List<String>)

    @Query("UPDATE UserApp SET name = :newName WHERE packageName = :packageName")
    suspend fun updateName(newName: String, packageName: String)

    @Query("UPDATE UserApp SET usageMillis = :usageTimeMillis WHERE packageName = :packageName")
    suspend fun updateUsageTime(usageTimeMillis: Long, packageName: String)

    @Query("UPDATE UserApp SET notificationCount = :notificationCount WHERE packageName = :packageName")
    suspend fun updateNotificationCount(notificationCount: Int, packageName: String)

    @Query("UPDATE UserApp SET notificationCount = 0")
    suspend fun unsetAllNotificationCount()

    @Query("Update UserApp SET `index` = :toIndex WHERE packageName = :packageName")
    suspend fun updateIndexByPackageName(packageName: String, toIndex: Int)

    @Query("SELECT MAX(`index`) FROM UserApp")
    suspend fun getLatestIndex(): Int
}