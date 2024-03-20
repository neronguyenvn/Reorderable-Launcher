package com.example.customlauncher.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.customlauncher.core.data.util.AppChangeBroadcastReceiver
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ClApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var imageLoader: Lazy<ImageLoader>

    @Inject
    lateinit var appChangeBroadcastReceiver: AppChangeBroadcastReceiver

    override fun newImageLoader(): ImageLoader = imageLoader.get()
}