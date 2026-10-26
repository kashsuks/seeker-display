package com.kashsuks.seekerdisplay.ui.dashboard

import com.kashsuks.seekerdisplay.data.calendar.CalendarEvent
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot
import com.kashsuks.seekerdisplay.data.weather.WeatherData
import java.time.ZonedDateTime

data class DashboardState(
    val now: ZonedDateTime,
    val weather: WeatherData? = null,
    val events: List<CalendarEvent> = emptyList(),
    val calendarPermission: Boolean = false,
    val playback: PlaybackSnapshot? = null,
)
