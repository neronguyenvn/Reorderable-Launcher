package com.example.customlauncher.core.database.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.customlauncher.core.database.AppDao
import com.example.customlauncher.core.database.model.AppEntity

@Database(
    entities = [AppEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RoomClDatabase : RoomDatabase() {
    abstract fun applicationDao(): AppDao
}
