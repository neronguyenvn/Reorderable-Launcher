package com.example.customlauncher.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.customlauncher.core.database.model.CompanyAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyAppDao {

    @Query("SELECT * FROM CompanyApp")
    fun observeAll(): Flow<List<CompanyAppEntity>>

    @Query("SELECT * FROM CompanyApp")
    suspend fun getAll(): List<CompanyAppEntity>

    @Upsert
    suspend fun upsert(companyAppEntity: CompanyAppEntity)

    @Query("UPDATE CompanyApp SET `index` = :toIndex WHERE packageName = :packageName")
    suspend fun updateIndexByPackageName(toIndex: Int, packageName: String)

    @Query("UPDATE CompanyApp SET page = :toPage, `index` = :toIndex WHERE packageName = :packageName")
    suspend fun updatePageAndIndexByPackageName(toPage: Int, toIndex: Int, packageName: String)

    @Query("DELETE FROM CompanyApp WHERE packageName NOT IN (:packages)")
    suspend fun deleteUninstalled(packages: List<String>)
}