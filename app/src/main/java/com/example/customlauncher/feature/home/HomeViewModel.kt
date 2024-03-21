package com.example.customlauncher.feature.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.PrimaryKey
import com.example.customlauncher.core.data.ApplicationRepository
import com.example.customlauncher.core.database.CompanyAppDao
import com.example.customlauncher.core.database.model.CompanyAppEntity
import com.example.customlauncher.core.model.Application
import com.example.customlauncher.feature.home.HomeScreenEvent.EditName
import com.example.customlauncher.feature.home.HomeScreenEvent.LongClickOnApp
import com.example.customlauncher.feature.home.HomeScreenEvent.MoveApp
import com.example.customlauncher.feature.home.HomeScreenEvent.StartDrag
import com.example.customlauncher.feature.home.HomeScreenEvent.StopDrag
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import javax.inject.Inject

data class HomeUiState(
    val applications: List<Application> = emptyList(),
    val selectedApplication: Application.UserApp? = null,
    val eventSink: (HomeScreenEvent) -> Unit = {}
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepo: ApplicationRepository,
    private val companyAppDao: CompanyAppDao
) : ViewModel() {


    private val _selectedApp = MutableStateFlow<Application.UserApp?>(null)
    private val selected get() = _selectedApp.value!!

    private var applicationCollectJob: Job? = null
    private var updateAppPositionJob: Job? = null
    private val _applications = MutableStateFlow<List<Application>>(emptyList())
    private val apps get() = _applications.value
    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded = _isDataLoaded.asStateFlow()

    init {
        startCollect()
        runBlocking { fetchDataFromApi() }
    }

    private suspend fun fetchDataFromApi() {
        viewModelScope.launch {
            val client = HttpClient(CIO) {
                expectSuccess = true
            }

            val response: HttpResponse =
                client.get("https://gitlab.com/QuangFiIt/upload_app/-/raw/main/json/default_test_2.0.0.json")
            AddCompanyApp(response.bodyAsText())
            client.close()
            delay(2000)
            _isDataLoaded.value = true
        }
    }

    suspend fun AddCompanyApp(json: String) {
        val jsonObject = JSONObject(json)
        val favoritesArray = jsonObject.getJSONArray("favorites")
        for (i in 0 until favoritesArray.length()) {
            var name = ""
            if (favoritesArray.getJSONObject(i).has("name")) {
                name = favoritesArray.getJSONObject(i).getString("name")
            }

            var hash = ""
            if (favoritesArray.getJSONObject(i).has("hash")) {
                hash = favoritesArray.getJSONObject(i).getString("hash")
            }

            var pubKey = ""
            if (favoritesArray.getJSONObject(i).has("pubKey")) {
                pubKey = favoritesArray.getJSONObject(i).getString("pubKey")
            }

            var sign = ""
            if (favoritesArray.getJSONObject(i).has("sign")) {
                sign = favoritesArray.getJSONObject(i).getString("sign")
            }

            var version = ""
            if (favoritesArray.getJSONObject(i).has("version")) {
                version = favoritesArray.getJSONObject(i).getString("version")
            }

            var logo = ""
            if (favoritesArray.getJSONObject(i).has("logo")) {
                logo = favoritesArray.getJSONObject(i).getString("logo")
            }

            var orientation = ""
            if (favoritesArray.getJSONObject(i).has("orientation")) {
                orientation = favoritesArray.getJSONObject(i).getString("orientation")
            }

            var author = ""
            if (favoritesArray.getJSONObject(i).has("author")) {
                author = favoritesArray.getJSONObject(i).getString("author")
            }

            var full_screen = false
            if (favoritesArray.getJSONObject(i).has("full_screen")) {
                full_screen = favoritesArray.getJSONObject(i).getBoolean("full_screen")
            }

            var status_bar = ""
            if (favoritesArray.getJSONObject(i).has("status_bar")) {
                status_bar = favoritesArray.getJSONObject(i).getString("status_bar")
            }

            var type = 0
            if (favoritesArray.getJSONObject(i).has("type")) {
                type = favoritesArray.getJSONObject(i).getInt("type")
            }

            var page = 0
            if (favoritesArray.getJSONObject(i).has("page")) {
                page = favoritesArray.getJSONObject(i).getInt("page")
            }

            var urlWeb: String = ""
            if (favoritesArray.getJSONObject(i).has("urlWeb")) {
                urlWeb = favoritesArray.getJSONObject(i).getString("urlWeb")
            }
            
            val id: String = favoritesArray.getJSONObject(i).getString("id")
            companyAppDao.upsert(
                CompanyAppEntity(
                    name, hash, pubKey, sign, version, logo, orientation,
                    author, full_screen, status_bar, type, page, urlWeb, true, id
                )
            )
        }
        val appsArray = jsonObject.getJSONArray("apps")
        for (i in 0 until appsArray.length()) {
            var name = ""
            if (appsArray.getJSONObject(i).has("name")) {
                name = appsArray.getJSONObject(i).getString("name")
            }

            var hash = ""
            if (appsArray.getJSONObject(i).has("hash")) {
                hash = appsArray.getJSONObject(i).getString("hash")
            }

            var pubKey = ""
            if (appsArray.getJSONObject(i).has("pubKey")) {
                pubKey = appsArray.getJSONObject(i).getString("pubKey")
            }

            var sign = ""
            if (appsArray.getJSONObject(i).has("sign")) {
                sign = appsArray.getJSONObject(i).getString("sign")
            }

            var version = ""
            if (appsArray.getJSONObject(i).has("version")) {
                version = appsArray.getJSONObject(i).getString("version")
            }

            var logo = ""
            if (appsArray.getJSONObject(i).has("logo")) {
                logo = appsArray.getJSONObject(i).getString("logo")
            }

            var orientation = ""
            if (appsArray.getJSONObject(i).has("orientation")) {
                orientation = appsArray.getJSONObject(i).getString("orientation")
            }

            var author = ""
            if (appsArray.getJSONObject(i).has("author")) {
                author = appsArray.getJSONObject(i).getString("author")
            }

            var full_screen = false
            if (appsArray.getJSONObject(i).has("full_screen")) {
                full_screen = appsArray.getJSONObject(i).getBoolean("full_screen")
            }

            var status_bar = ""
            if (appsArray.getJSONObject(i).has("status_bar")) {
                status_bar = appsArray.getJSONObject(i).getString("status_bar")
            }

            var type = 0
            if (appsArray.getJSONObject(i).has("type")) {
                type = appsArray.getJSONObject(i).getInt("type")
            }

            var page = 0
            if (appsArray.getJSONObject(i).has("page")) {
                page = appsArray.getJSONObject(i).getInt("page")
            }

            var urlWeb: String = ""
            if (appsArray.getJSONObject(i).has("urlWeb")) {
                urlWeb = appsArray.getJSONObject(i).getString("urlWeb")
            }

            val id: String = appsArray.getJSONObject(i).getString("id")
            companyAppDao.upsert(
                CompanyAppEntity(
                    name, hash, pubKey, sign, version, logo, orientation,
                    author, full_screen, status_bar, type, page, urlWeb, false, id
                )
            )
        }


    }

    private val eventSink: (HomeScreenEvent) -> Unit = { event ->
        when (event) {
            is LongClickOnApp -> _selectedApp.value = event.userApp
            is EditName -> viewModelScope.launch {
                appRepo.editName(event.value, selected)
                _selectedApp.value = null
            }

            is MoveApp -> _applications.value = apps.toMutableList()
                .apply { add(event.to.index, removeAt(event.from.index)) }

            is StartDrag -> cancelAllJobs()

            is StopDrag -> updateAppPositionJob = viewModelScope.launch {
                delay(500)
                for (i in minOf(event.from, event.to)..apps.lastIndex) {
                    appRepo.moveApplication(apps[i].packageName, i)
                }
                startCollect()
            }
        }
    }

    val uiState = _applications.combine(_selectedApp) { apps, selected ->
        HomeUiState(
            applications = apps,
            selectedApplication = selected,
            eventSink = eventSink
        )
    }.stateIn(
        scope = viewModelScope,
        initialValue = HomeUiState(),
        started = SharingStarted.WhileSubscribed(5000)
    )

    private fun startCollect() {
        applicationCollectJob = appRepo.getApplicationsStream()
            .onEach { _applications.value = it.filterNotNull() }
            .launchIn(viewModelScope)
    }

    private fun cancelAllJobs() {
        applicationCollectJob?.cancel()
        applicationCollectJob = null
        updateAppPositionJob?.cancel()
        updateAppPositionJob = null
    }


}