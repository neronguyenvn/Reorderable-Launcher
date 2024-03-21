package com.example.customlauncher.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.customlauncher.core.database.room.RoomClDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): RoomClDatabase = Room.databaseBuilder(
        context,
        RoomClDatabase::class.java,
        "cl-database"
    ).addMigrations(RoomClDatabase.migration_from_1_to_2).build()

    @Provides
    fun providesApplicationDao(
        database: RoomClDatabase
    ) = database.applicationDao()
    @Provides
    fun providesCompanyApplicationDao(
        database: RoomClDatabase
    ) = database.companyApplicationDao()
}