package com.example.customlauncher.core.database.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.customlauncher.core.database.ApplicationDao
import com.example.customlauncher.core.database.CompanyAppDao
import com.example.customlauncher.core.database.model.CompanyAppEntity
import com.example.customlauncher.core.database.model.UserAppEntity

@Database(
    entities = [
        UserAppEntity::class, CompanyAppEntity::class

    ],
    version = 2,
    exportSchema = false
)
abstract class RoomClDatabase : RoomDatabase() {
    companion object {
        var migration_from_1_to_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {

                // Thêm bảng CompanyApp
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `CompanyApp`" +
                            " (`id` TEXT PRIMARY KEY NOT NULL, " +
                            "`name` TEXT , " +
                            "`hash` TEXT , " +
                            "`pubKey` TEXT , " +
                            "`sign` TEXT , " +
                            "`version` TEXT , " +
                            "`logo` TEXT , " +
                            "`orientation` TEXT , " +
                            "`author` TEXT , " +
                            "`full_screen` INTEGER , " +
                            "`status_bar` TEXT , " +
                            "`type` INTEGER , " +
                            "`page` INTEGER , " +
                            "`urlWeb` TEXT , " +
                            "`isFavorite` INTEGER)"
                )
            }
        }
    }

    abstract fun applicationDao(): ApplicationDao
    abstract fun companyApplicationDao(): CompanyAppDao
}