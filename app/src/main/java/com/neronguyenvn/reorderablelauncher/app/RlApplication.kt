package com.neronguyenvn.reorderablelauncher.app

import android.app.Application
import com.neronguyenvn.reorderablelauncher.core.data.broadcast.AppChangeBroadcastReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RlApplication : Application() {

    @Inject
    lateinit var appChangeBroadcastReceiver: AppChangeBroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        appChangeBroadcastReceiver.register(this)
    }
}