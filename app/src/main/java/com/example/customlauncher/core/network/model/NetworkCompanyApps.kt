package com.example.customlauncher.core.network.model

import com.example.customlauncher.core.database.model.CompanyAppEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkCompanyApps(
    val version: String,
    val favorites: List<NetworkCompanyApp>,
    val apps: List<NetworkCompanyApp>
)

@Serializable
data class NetworkCompanyApp(
    val name: String,
    val version: String,
    val urlWeb: String? = null,
    val logo: String,

    @SerialName("id")
    val packageName: String,
    val isLocal: Boolean? = null,

    @SerialName("full_screen")
    val fullScreen: Boolean? = null,
    val type: Long,
    val urlScript: String? = null,
    val page: Long,
    val hash: String? = null,
    val pubKey: String? = null,
    val sign: String? = null,
    val urlZip: String? = null,
    val urlLauchScreen: String? = null,
    val urlLoadingScreen: String? = null
)

fun NetworkCompanyApp.asEntity(index: Int, page: Int) = CompanyAppEntity(
    name = name,
    version = version,
    urlWeb = urlWeb ?: "",
    logo = logo,
    type = type,
    index = index,
    page = page,
    packageName = packageName,
)