package com.example.customlauncher.core.data.util

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.customlauncher.core.common.coroutine.di.ApplicationScope
import com.example.customlauncher.core.data.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotiListener : NotificationListenerService() {

    @Inject
    lateinit var appRepo: AppRepository

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    private var isConnected = false

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (isConnected) {
            requestApplicationListUpdate(activeNotifications.toList())
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (isConnected) {
            requestApplicationListUpdate(activeNotifications.toList())
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        requestApplicationListUpdate(activeNotifications.toList())
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    private fun requestApplicationListUpdate(notifications: List<StatusBarNotification>) {
        appScope.launch {
            appRepo.handleNotis(notifications)
        }
    }
}