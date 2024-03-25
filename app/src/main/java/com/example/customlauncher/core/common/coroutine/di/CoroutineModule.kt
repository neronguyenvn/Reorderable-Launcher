package com.example.customlauncher.core.common.coroutine.di

import com.example.customlauncher.core.common.coroutine.ClDispatcher.Default
import com.example.customlauncher.core.common.coroutine.ClDispatcher.IO
import com.example.customlauncher.core.common.coroutine.Dispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope


@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Dispatcher(IO)
    fun providesIODispatcher() = Dispatchers.IO


    @Provides
    @Dispatcher(Default)
    fun providesDefaultDispatcher() = Dispatchers.Default

    @Provides
    @Singleton
    @ApplicationScope
    fun providesCoroutineScope(
        @Dispatcher(Default) dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}