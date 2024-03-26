package com.example.customlauncher.core.network

import com.example.customlauncher.core.network.model.NetworkCompanyApps

interface ClNetworkDataSource {

    suspend fun getCompanyApps(): NetworkCompanyApps

    suspend fun sendCurrentTime()
}