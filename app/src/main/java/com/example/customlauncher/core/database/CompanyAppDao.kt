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

    @Upsert
    suspend fun upsert(companyAppEntity: CompanyAppEntity)

    @Query("SELECT MAX(`index`) FROM UserApp")
    suspend fun getLatestIndex(): Int

    @Query("UPDATE CompanyApp SET `index` = :toIndex WHERE packageName = :packageName")
    suspend fun updateIndexById(toIndex: Int, packageName: String)
}