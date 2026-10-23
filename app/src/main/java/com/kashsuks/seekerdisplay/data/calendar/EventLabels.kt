package com.kashsuks.seekerdisplay.data.calendar

import com.kashsuks.seekerdisplay.core.TimeFormat
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Pure text helpers for the Up Next list, kept free of Android types so they are unit-testable. */
object EventLabels {
    private val fullWeekday = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())

    private fun at(ms: Long, now: ZonedDateTime): ZonedDateTime = Instant.ofEpochMilli(ms).atZone(now.zone)

    private fun dayName(date: ZonedDateTime, now: ZonedDateTime): String {
        val diff = date.toLocalDate().toEpochDay() - now.toLocalDate().toEpochDay()
        return when (diff) {
            0L -> "Today"
            1L -> "Tomorrow"
            else -> date.format(fullWeekday)
        }
    }

    fun isInProgress(event: CalendarEvent, nowMs: Long): Boolean =
        !event.allDay && event.beginMs <= nowMs && nowMs < event.endMs

    /** Second line under the first event, e.g. "Monday, 12:30 – 13:20" or "Now · until 13:20". */
    fun subtitle(event: CalendarEvent, now: ZonedDateTime): String {
        val nowMs = now.toInstant().toEpochMilli()
        val begin = at(event.beginMs, now)
        val end = at(event.endMs, now)
        return when {
            event.allDay -> "${dayName(begin, now)}, All day"
            isInProgress(event, nowMs) -> "Now · until ${TimeFormat.time(end)}"
            else -> "${dayName(begin, now)}, ${TimeFormat.time(begin)} – ${TimeFormat.time(end)}"
        }
    }

    /** Short right-aligned label for compact rows: "Now", a start time for today, else "Mon". */
    fun dayLabel(event: CalendarEvent, now: ZonedDateTime): String {
        val nowMs = now.toInstant().toEpochMilli()
        val begin = at(event.beginMs, now)
        val today = begin.toLocalDate() == now.toLocalDate()
        return when {
            isInProgress(event, nowMs) -> "Now"
            event.allDay -> if (today) "All day" else TimeFormat.shortWeekday(begin)
            today -> TimeFormat.time(begin)
            else -> TimeFormat.shortWeekday(begin)
        }
    }
}
