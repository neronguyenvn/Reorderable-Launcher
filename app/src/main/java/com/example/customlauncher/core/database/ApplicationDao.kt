package com.example.customlauncher.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.example.customlauncher.core.database.model.ApplicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {

    @Query("SELECT * FROM Application")
    fun observeAll(): Flow<List<ApplicationEntity>>

    @Upsert
    suspend fun upsert(applicationEntity: ApplicationEntity)

    @Delete
    suspend fun delete(applicationEntity: ApplicationEntity)
}