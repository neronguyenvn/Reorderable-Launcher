package com.example.customlauncher.app

import android.app.Application
import com.example.customlauncher.core.data.broadcast.AppChangeBroadcastReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ClApplication : Application() {

    @Inject
    lateinit var appChangeBroadcastReceiver: AppChangeBroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        appChangeBroadcastReceiver.register(this)
    }
}