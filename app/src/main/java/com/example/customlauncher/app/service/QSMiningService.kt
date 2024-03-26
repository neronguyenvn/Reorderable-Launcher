package com.example.customlauncher.app.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.customlauncher.R
import com.example.customlauncher.core.common.coroutine.di.ApplicationScope
import com.example.customlauncher.core.data.UserDataRepository
import com.example.customlauncher.core.network.ClNetworkDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QSMiningService : TileService() {

    @Inject
    lateinit var userDataRepository: UserDataRepository

    @Inject
    @ApplicationScope
    lateinit var appScope: CoroutineScope

    @Inject
    lateinit var network: ClNetworkDataSource

    private var miningJob: Job? = null

    override fun onStartListening() {
        miningJob = appScope.launch {
            while (true) {
                runCatching { network.sendCurrentTime() }
                delay(3000)
            }
        }
    }

    override fun onStopListening() {
        miningJob?.cancel()
        miningJob = null
    }

    override fun onClick() {
        updateTile()
    }

    private fun updateTile() = appScope.launch {
        val tile = this@QSMiningService.qsTile
        val isActive: Boolean = getServiceStatus()
        val newIcon: Icon
        val newState: Int

        if (isActive) {
            newIcon = Icon.createWithResource(
                applicationContext,
                R.drawable.ic_launcher_foreground
            )
            newState = Tile.STATE_ACTIVE
        } else {
            newIcon = Icon.createWithResource(
                applicationContext,
                android.R.drawable.stat_sys_warning
            )
            newState = Tile.STATE_INACTIVE
        }

        tile.icon = newIcon
        tile.state = newState
        tile.updateTile()
    }

    private suspend fun getServiceStatus(): Boolean {
        var isActive = userDataRepository.userData.first().isMiningOn
        isActive = !isActive
        userDataRepository.setMining(isActive)
        return isActive
    }
}