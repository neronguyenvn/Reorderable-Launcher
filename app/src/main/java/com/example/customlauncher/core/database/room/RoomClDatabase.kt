package com.example.customlauncher.core.database.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.customlauncher.core.database.ApplicationDao
import com.example.customlauncher.core.database.model.UserAppEntity

@Database(
    entities = [
        UserAppEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RoomClDatabase : RoomDatabase() {
    abstract fun applicationDao(): ApplicationDao
}