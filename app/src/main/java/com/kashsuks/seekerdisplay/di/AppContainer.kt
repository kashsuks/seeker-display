package com.kashsuks.seekerdisplay.di

import android.content.Context
import com.kashsuks.seekerdisplay.data.bridge.BridgeDiscovery
import com.kashsuks.seekerdisplay.data.bridge.MacBridgeSource
import com.kashsuks.seekerdisplay.data.calendar.CalendarRepository
import com.kashsuks.seekerdisplay.data.location.CityResolver
import com.kashsuks.seekerdisplay.data.location.LocationProvider
import com.kashsuks.seekerdisplay.data.media.LocalMediaSessionSource
import com.kashsuks.seekerdisplay.data.media.PlaybackRepository
import com.kashsuks.seekerdisplay.data.settings.SettingsRepository
import com.kashsuks.seekerdisplay.data.weather.OpenMeteoApi
import com.kashsuks.seekerdisplay.data.weather.WeatherRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Hand-rolled dependency container, created once by [com.kashsuks.seekerdisplay.SeekerApp]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val json = Json { ignoreUnknownKeys = true }

    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
        }
    }

    val settings = SettingsRepository(appContext)
    val locationProvider = LocationProvider(appContext)

    val weather = WeatherRepository(
        api = OpenMeteoApi(httpClient),
        location = locationProvider,
        cities = CityResolver(appContext, httpClient),
        settings = settings,
        json = json,
    )

    val calendar = CalendarRepository(appContext, settings)

    val phoneMedia = LocalMediaSessionSource(appContext)
    val macBridge = MacBridgeSource(httpClient, settings, BridgeDiscovery(appContext))
    val playback = PlaybackRepository(listOf(phoneMedia, macBridge))
}
