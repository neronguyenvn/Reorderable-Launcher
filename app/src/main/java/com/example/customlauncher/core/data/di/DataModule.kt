package com.example.customlauncher.core.data.di

import com.example.customlauncher.core.data.ApplicationRepository
import com.example.customlauncher.core.data.implementation.OfflineFirstApplicationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindsApplicationRepository(
        applicationRepository: OfflineFirstApplicationRepository
    ): ApplicationRepository
}