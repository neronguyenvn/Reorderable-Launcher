package com.neronguyenvn.reorderablelauncher.core.data.di

import com.neronguyenvn.reorderablelauncher.core.data.AppRepository
import com.neronguyenvn.reorderablelauncher.core.data.repository.OfflineFirstAppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindsApplicationRepository(
        appRepo: OfflineFirstAppRepository
    ): AppRepository
}
