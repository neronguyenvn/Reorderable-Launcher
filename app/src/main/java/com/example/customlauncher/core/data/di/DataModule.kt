package com.example.customlauncher.core.data.di

import com.example.customlauncher.core.data.AppRepository
import com.example.customlauncher.core.data.repository.OfflineFirstAppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindsApplicationRepository(
        appRepo: OfflineFirstAppRepository
    ): AppRepository
}