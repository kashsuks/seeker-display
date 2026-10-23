package com.kashsuks.seekerdisplay.core

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** All user-facing time strings, 24-hour. */
object TimeFormat {
    private val weekday = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
    private val time = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val date = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
    private val shortWeekday = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val hour = DateTimeFormatter.ofPattern("HH", Locale.getDefault())

    fun weekday(t: ZonedDateTime): String = t.format(weekday).uppercase(Locale.getDefault())
    fun time(t: ZonedDateTime): String = t.format(time)
    fun date(t: ZonedDateTime): String = t.format(date)
    fun shortWeekday(t: ZonedDateTime): String = t.format(shortWeekday)
    fun hour(t: ZonedDateTime): String = t.format(hour)
    fun hourOfDay(hour: Int): String = "%02d".format(hour)

    /** m:ss for track positions. */
    fun duration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
}
