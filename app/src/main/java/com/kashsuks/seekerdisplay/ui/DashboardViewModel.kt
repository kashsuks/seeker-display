package com.kashsuks.seekerdisplay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kashsuks.seekerdisplay.data.calendar.CalendarEvent
import com.kashsuks.seekerdisplay.data.calendar.DeviceCalendar
import com.kashsuks.seekerdisplay.data.media.ActivePlayback
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot
import com.kashsuks.seekerdisplay.data.settings.DisplaySettings
import com.kashsuks.seekerdisplay.data.weather.WeatherData
import com.kashsuks.seekerdisplay.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(private val container: AppContainer) : ViewModel() {
    val weather: StateFlow<WeatherData?> = container.weather.weather
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val events: StateFlow<List<CalendarEvent>> = container.calendar.upcoming
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _calendarPermission = MutableStateFlow(container.calendar.hasPermission())
    val calendarPermission: StateFlow<Boolean> = _calendarPermission.asStateFlow()

    private val activePlayback: StateFlow<ActivePlayback?> = container.playback.active
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val playback: StateFlow<PlaybackSnapshot?> = activePlayback.map { it?.snapshot }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun playPause() = withSource { it.playPause() }
    fun next() = withSource { it.next() }
    fun previous() = withSource { it.previous() }
    fun seekTo(positionMs: Long) = withSource { it.seekTo(positionMs) }
    fun setShuffle(enabled: Boolean) = withSource { it.setShuffle(enabled) }
    fun setRepeat(enabled: Boolean) = withSource { it.setRepeat(enabled) }

    /** Speeds up polling while Now Playing is on screen. */
    fun setNowPlayingVisible(visible: Boolean) = container.playback.setFastPolling(visible)

    private fun withSource(block: suspend (com.kashsuks.seekerdisplay.data.media.MediaSource) -> Unit) {
        val source = activePlayback.value?.source ?: return
        viewModelScope.launch {
            // A dropped Wi-Fi connection mid-press must not crash the app; the next poll corrects the UI.
            try {
                block(source)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    private val _calendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
    val calendars: StateFlow<List<DeviceCalendar>> = _calendars.asStateFlow()

    val hiddenCalendarIds: StateFlow<Set<Long>> = container.settings.hiddenCalendarIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Reads the device's calendar list; called when Settings opens. */
    fun loadCalendars() {
        viewModelScope.launch { _calendars.value = container.calendar.calendars() }
    }

    fun setCalendarVisible(calendarId: Long, visible: Boolean) {
        viewModelScope.launch { container.settings.setCalendarHidden(calendarId, hidden = !visible) }
    }

    val display: StateFlow<DisplaySettings> = container.settings.display
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplaySettings())

    fun setShowSeconds(enabled: Boolean) {
        viewModelScope.launch { container.settings.setShowSeconds(enabled) }
    }

    fun setNightDimEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settings.setNightDimEnabled(enabled) }
    }

    fun setNightWindow(startMinute: Int, endMinute: Int) {
        viewModelScope.launch { container.settings.setNightWindow(startMinute, endMinute) }
    }

    val bridgeAddress: StateFlow<String> = container.settings.bridgeAddress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val bridgeToken: StateFlow<String> = container.settings.bridgeToken
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _bridgeTestResult = MutableStateFlow<String?>(null)
    val bridgeTestResult: StateFlow<String?> = _bridgeTestResult.asStateFlow()

    fun setBridgeAddress(address: String) {
        _bridgeTestResult.value = null
        viewModelScope.launch { container.settings.setBridgeAddress(address) }
    }

    fun setBridgeToken(token: String) {
        _bridgeTestResult.value = null
        viewModelScope.launch { container.settings.setBridgeToken(token) }
    }

    fun testBridge() {
        _bridgeTestResult.value = "Testing…"
        viewModelScope.launch { _bridgeTestResult.value = container.macBridge.testConnection() }
    }

    fun isNotificationAccessGranted(): Boolean = container.phoneMedia.isAccessGranted()

    /** Call after runtime permissions change so sources can retry straight away. */
    fun onPermissionsChanged() {
        _calendarPermission.value = container.calendar.hasPermission()
        container.weather.refreshNow()
        container.calendar.refreshNow()
        container.phoneMedia.restart()
        loadCalendars()
    }
}
