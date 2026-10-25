package com.kashsuks.seekerdisplay.core

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** All user-facing time strings, 24-hour. Formatters are built per call so a locale change takes effect. */
object TimeFormat {
    private fun weekdayFormat() = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
    private fun timeFormat() = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private fun dateFormat() = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
    private fun shortWeekdayFormat() = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private fun hourFormat() = DateTimeFormatter.ofPattern("HH", Locale.getDefault())

    fun weekday(t: ZonedDateTime): String = t.format(weekdayFormat()).uppercase(Locale.getDefault())
    fun time(t: ZonedDateTime): String = t.format(timeFormat())
    fun date(t: ZonedDateTime): String = t.format(dateFormat())
    fun shortWeekday(t: ZonedDateTime): String = t.format(shortWeekdayFormat())
    fun hour(t: ZonedDateTime): String = t.format(hourFormat())
    fun hourOfDay(hour: Int): String = "%02d".format(hour)

    /** m:ss for track positions. */
    fun duration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
}
