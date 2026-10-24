package com.kashsuks.seekerdisplay.data.calendar

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** An event instance exactly as the calendar provider reports it. */
data class RawInstance(
    val eventId: Long,
    val title: String?,
    val beginMs: Long,
    val endMs: Long,
    val allDay: Boolean,
    /** Per-event colour override, or 0 when unset. */
    val eventColor: Int,
    val calendarColor: Int,
    val calendarId: Long,
    val selfAttendeeStatus: Int,
)

/** Filters, normalizes and orders raw calendar instances into what the Up Next list shows. */
object UpcomingEvents {
    /** CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED */
    private const val STATUS_DECLINED = 2
    const val DEFAULT_LIMIT = 8

    fun build(
        raw: List<RawInstance>,
        nowMs: Long,
        zone: ZoneId,
        hiddenCalendarIds: Set<Long>,
        limit: Int = DEFAULT_LIMIT,
    ): List<CalendarEvent> =
        raw.asSequence()
            .filter { it.calendarId !in hiddenCalendarIds && it.selfAttendeeStatus != STATUS_DECLINED }
            .map { normalize(it, zone) }
            .filter { it.endMs > nowMs }
            .distinctBy { Triple(it.title, it.beginMs, it.endMs) }
            .sortedWith(
                compareBy<CalendarEvent> { Instant.ofEpochMilli(it.beginMs).atZone(zone).toLocalDate().coerceAtLeast(today(nowMs, zone)) }
                    .thenBy { !it.allDay }
                    .thenBy { it.beginMs },
            )
            .take(limit)
            .toList()

    private fun today(nowMs: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

    private fun normalize(raw: RawInstance, zone: ZoneId): CalendarEvent {
        // All-day events are stored as UTC midnights; reinterpret them as local midnights.
        val begin = if (raw.allDay) utcMidnightToLocal(raw.beginMs, zone) else raw.beginMs
        val end = if (raw.allDay) utcMidnightToLocal(raw.endMs, zone) else raw.endMs
        val color = (if (raw.eventColor != 0) raw.eventColor else raw.calendarColor) or OPAQUE
        return CalendarEvent(
            id = raw.eventId,
            title = raw.title?.takeIf { it.isNotBlank() } ?: "(No title)",
            beginMs = begin,
            endMs = end,
            allDay = raw.allDay,
            color = color,
            calendarId = raw.calendarId,
        )
    }

    private fun utcMidnightToLocal(ms: Long, zone: ZoneId): Long =
        Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    private const val OPAQUE = 0xFF000000.toInt()
}
