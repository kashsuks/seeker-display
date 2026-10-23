package com.kashsuks.seekerdisplay.ui.preview

import com.kashsuks.seekerdisplay.data.calendar.CalendarEvent
import com.kashsuks.seekerdisplay.data.media.MediaSourceType
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot
import com.kashsuks.seekerdisplay.data.weather.HourlySlot
import com.kashsuks.seekerdisplay.data.weather.WeatherCondition
import com.kashsuks.seekerdisplay.data.weather.WeatherData
import com.kashsuks.seekerdisplay.data.weather.WeatherSymbol
import java.time.ZonedDateTime

/** Static data used for previews and before real sources are wired in. */
object PlaceholderData {
    private val clearNight = WeatherSymbol(WeatherCondition.CLEAR, isDay = false)
    private val cloudy = WeatherSymbol(WeatherCondition.CLOUDY, isDay = false)
    private val sunny = WeatherSymbol(WeatherCondition.CLEAR, isDay = true)

    val weather = WeatherData(
        city = "Waterloo",
        tempC = 9,
        feelsLikeC = 6,
        highC = 16,
        lowC = 5,
        symbol = clearNight,
        label = "Clear",
        hourly = listOf(
            HourlySlot(null, 9, clearNight),
            HourlySlot(22, 8, clearNight),
            HourlySlot(0, 7, clearNight),
            HourlySlot(2, 6, cloudy),
            HourlySlot(4, 6, cloudy),
            HourlySlot(6, 5, cloudy),
            HourlySlot(8, 5, sunny),
        ),
    )

    fun events(now: ZonedDateTime): List<CalendarEvent> {
        val base = now.plusDays(2).withHour(12).withMinute(30).withSecond(0).withNano(0).toInstant().toEpochMilli()
        fun event(id: Long, title: String, offsetMin: Long, color: Int) =
            CalendarEvent(id, title, base + offsetMin * 60_000, base + (offsetMin + 50) * 60_000, false, color, 1)
        return listOf(
            event(1, "MATH 137 (LEC)", 0, 0xFFE5D44B.toInt()),
            event(2, "MATH 135 (LEC)", 60, 0xFFE5D44B.toInt()),
            event(3, "YSWS Start Of Week Meeting", 120, 0xFFFF2D95.toInt()),
            event(4, "MATH 137 Quiz 3", 180, 0xFFE5D44B.toInt()),
            event(5, "MATH 137 (TUT)", 240, 0xFFE5D44B.toInt()),
        )
    }

    val playback = PlaybackSnapshot(
        source = MediaSourceType.PHONE,
        sourceLabel = "On this phone",
        isPlaying = true,
        title = "Nights",
        artist = "Frank Ocean",
        album = "Blonde",
        durationMs = 307_000,
        positionMs = 102_000,
    )
}
