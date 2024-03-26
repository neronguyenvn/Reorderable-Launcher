package com.example.customlauncher.core.network.ktor

import com.example.customlauncher.core.network.ClNetworkDataSource
import com.example.customlauncher.core.network.model.NetworkCompanyApps
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import java.time.Instant

class KtorNetwork(
    private val httpClient: HttpClient,
    private val json: Json
) : ClNetworkDataSource {

    override suspend fun getCompanyApps(): NetworkCompanyApps {
        val jsonString = httpClient.get(URL).bodyAsText()
        return json.decodeFromString(jsonString)
    }

    override suspend fun sendCurrentTime() {
        val currentTime = Instant.now().toString()
        httpClient.post("http://192.168.59.243:8080/requestTime") {
            setBody(currentTime)
        }
    }
}

private const val URL =
    "https://gitlab.com/QuangFiIt/upload_app/-/raw/main/json/default_test_2.0.0.json"