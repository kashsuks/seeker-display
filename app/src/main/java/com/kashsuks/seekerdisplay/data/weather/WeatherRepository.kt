package com.kashsuks.seekerdisplay.data.weather

import android.location.Location
import com.kashsuks.seekerdisplay.data.location.CityResolver
import com.kashsuks.seekerdisplay.data.location.LocationProvider
import com.kashsuks.seekerdisplay.data.settings.SavedLocation
import com.kashsuks.seekerdisplay.data.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import kotlinx.serialization.json.Json

/**
 * Serves current weather for wherever the phone is. Emits the cached forecast first (marked stale),
 * then a fresh one every [REFRESH_MS]. On failure it keeps showing the last data, marked stale.
 */
class WeatherRepository(
    private val api: OpenMeteoApi,
    private val location: LocationProvider,
    private val cities: CityResolver,
    private val settings: SettingsRepository,
    private val json: Json,
) {
    private val kicks = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Restart the refresh loop immediately, e.g. right after location permission is granted. */
    fun refreshNow() {
        kicks.tryEmit(Unit)
    }

    val weather: Flow<WeatherData?> = kicks.onStart { emit(Unit) }.transformLatest {
        var last: WeatherData? = cached()?.copy(isStale = true)
        last?.let { emit(it) }
        while (true) {
            val fresh = fetch()
            if (fresh != null) {
                last = fresh
                emit(fresh)
                delay(REFRESH_MS)
            } else {
                last?.let { emit(it.copy(isStale = true)) }
                delay(RETRY_MS)
            }
        }
    }

    private suspend fun cached(): WeatherData? = runCatching {
        settings.weatherCache()?.let { json.decodeFromString<WeatherData>(it) }
    }.getOrNull()

    private suspend fun fetch(): WeatherData? {
        val place = resolvePlace() ?: return null
        return try {
            val data = WeatherMapper.map(api.forecast(place.latitude, place.longitude), place.city ?: FALLBACK_CITY)
            settings.saveWeatherCache(json.encodeToString(WeatherData.serializer(), data))
            data
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    /** Current location if available (reverse-geocoding only after moving), else the last saved one. */
    private suspend fun resolvePlace(): SavedLocation? {
        val saved = settings.savedLocation.first()
        val fix = location.currentLocation() ?: return saved

        val moved = saved == null || distanceMeters(saved, fix) > CITY_RECHECK_METERS
        val city = if (moved || saved?.city == null) {
            cities.cityName(fix.latitude, fix.longitude) ?: saved?.city
        } else {
            saved.city
        }
        return SavedLocation(fix.latitude, fix.longitude, city).also { settings.saveLocation(it) }
    }

    private fun distanceMeters(from: SavedLocation, to: Location): Float {
        val out = FloatArray(1)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, out)
        return out[0]
    }

    private companion object {
        const val REFRESH_MS = 15 * 60 * 1000L
        const val RETRY_MS = 2 * 60 * 1000L
        const val CITY_RECHECK_METERS = 5_000f
        const val FALLBACK_CITY = "Local"
    }
}
