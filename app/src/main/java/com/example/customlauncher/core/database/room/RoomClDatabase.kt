package com.example.customlauncher.core.database.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.customlauncher.core.database.CompanyAppDao
import com.example.customlauncher.core.database.UserAppDao
import com.example.customlauncher.core.database.model.CompanyAppEntity
import com.example.customlauncher.core.database.model.UserAppEntity

@Database(
    entities = [
        UserAppEntity::class,
        CompanyAppEntity::class

    ],
    version = 1,
    exportSchema = false
)
abstract class RoomClDatabase : RoomDatabase() {
    abstract fun applicationDao(): UserAppDao
    abstract fun companyApplicationDao(): CompanyAppDao
}