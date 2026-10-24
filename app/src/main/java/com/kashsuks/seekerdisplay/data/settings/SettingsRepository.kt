package com.kashsuks.seekerdisplay.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

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

    suspend fun weatherCache(): String? = store.data.first()[Keys.weatherJson]

    suspend fun saveWeatherCache(json: String) {
        store.edit { it[Keys.weatherJson] = json }
    }
}
