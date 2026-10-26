package com.kashsuks.seekerdisplay.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** How the screen behaves: focus clock seconds and overnight dimming. Times are minutes after midnight. */
data class DisplaySettings(
    val showSeconds: Boolean = false,
    val nightDimEnabled: Boolean = true,
    val nightStartMinute: Int = 23 * 60,
    val nightEndMinute: Int = 7 * 60,
)

/** Last location we resolved, so weather still has somewhere to look if GPS is slow or unavailable. */
data class SavedLocation(val latitude: Double, val longitude: Double, val city: String?)

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private object Keys {
        val latitude = doublePreferencesKey("last_latitude")
        val longitude = doublePreferencesKey("last_longitude")
        val city = stringPreferencesKey("last_city")
        val weatherJson = stringPreferencesKey("weather_cache_json")
        val hiddenCalendars = stringSetPreferencesKey("hidden_calendar_ids")
        val bridgeAddress = stringPreferencesKey("bridge_address")
        val bridgeToken = stringPreferencesKey("bridge_token")
        val showSeconds = booleanPreferencesKey("show_seconds")
        val nightDimEnabled = booleanPreferencesKey("night_dim_enabled")
        val nightStart = intPreferencesKey("night_start_minute")
        val nightEnd = intPreferencesKey("night_end_minute")
    }

    val savedLocation: Flow<SavedLocation?> = store.data.map { prefs ->
        val lat = prefs[Keys.latitude]
        val lon = prefs[Keys.longitude]
        if (lat != null && lon != null) SavedLocation(lat, lon, prefs[Keys.city]) else null
    }

    suspend fun saveLocation(location: SavedLocation) {
        store.edit { prefs ->
            prefs[Keys.latitude] = location.latitude
            prefs[Keys.longitude] = location.longitude
            if (location.city != null) prefs[Keys.city] = location.city else prefs.remove(Keys.city)
        }
    }

    /** Calendars the user switched off in Settings. Everything else is shown. */
    val hiddenCalendarIds: Flow<Set<Long>> = store.data.map { prefs ->
        prefs[Keys.hiddenCalendars].orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    }

    suspend fun setCalendarHidden(calendarId: Long, hidden: Boolean) {
        store.edit { prefs ->
            val current = prefs[Keys.hiddenCalendars].orEmpty()
            prefs[Keys.hiddenCalendars] = if (hidden) current + calendarId.toString() else current - calendarId.toString()
        }
    }

    /** Manual `host[:port]` for the Mac bridge; blank means find it automatically. */
    val bridgeAddress: Flow<String> = store.data.map { it[Keys.bridgeAddress].orEmpty() }
    val bridgeToken: Flow<String> = store.data.map { it[Keys.bridgeToken].orEmpty() }

    suspend fun setBridgeAddress(address: String) {
        store.edit { it[Keys.bridgeAddress] = address.trim() }
    }

    suspend fun setBridgeToken(token: String) {
        store.edit { it[Keys.bridgeToken] = token.trim() }
    }

    val display: Flow<DisplaySettings> = store.data.map { prefs ->
        val defaults = DisplaySettings()
        DisplaySettings(
            showSeconds = prefs[Keys.showSeconds] ?: defaults.showSeconds,
            nightDimEnabled = prefs[Keys.nightDimEnabled] ?: defaults.nightDimEnabled,
            nightStartMinute = prefs[Keys.nightStart] ?: defaults.nightStartMinute,
            nightEndMinute = prefs[Keys.nightEnd] ?: defaults.nightEndMinute,
        )
    }

    suspend fun setShowSeconds(enabled: Boolean) {
        store.edit { it[Keys.showSeconds] = enabled }
    }

    suspend fun setNightDimEnabled(enabled: Boolean) {
        store.edit { it[Keys.nightDimEnabled] = enabled }
    }

    suspend fun setNightWindow(startMinute: Int, endMinute: Int) {
        store.edit {
            it[Keys.nightStart] = startMinute
            it[Keys.nightEnd] = endMinute
        }
    }

    suspend fun weatherCache(): String? = store.data.first()[Keys.weatherJson]

    suspend fun saveWeatherCache(json: String) {
        store.edit { it[Keys.weatherJson] = json }
    }
}
