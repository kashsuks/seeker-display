package com.kashsuks.seekerdisplay.data.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class UpcomingEventsTest {
    private val zone = ZoneId.of("America/Toronto")
    private val now = LocalDateTime.parse("2026-10-03T21:24").atZone(zone).toInstant().toEpochMilli()

    private fun local(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun raw(
        id: Long,
        title: String? = "Event $id",
        begin: String,
        end: String,
        calendarId: Long = 1,
        status: Int = 0,
        eventColor: Int = 0,
        calendarColor: Int = 0x00112233,
    ) = RawInstance(id, title, local(begin), local(end), false, eventColor, calendarColor, calendarId, status)

    @Test
    fun dropsFinishedHiddenAndDeclinedEvents() {
        val events = UpcomingEvents.build(
            listOf(
                raw(1, begin = "2026-10-03T08:00", end = "2026-10-03T09:00"), // already over
                raw(2, begin = "2026-10-05T12:30", end = "2026-10-05T13:20"),
                raw(3, begin = "2026-10-05T14:00", end = "2026-10-05T15:00", calendarId = 9),
                raw(4, begin = "2026-10-05T16:00", end = "2026-10-05T17:00", status = 2),
            ),
            now, zone, hiddenCalendarIds = setOf(9L),
        )
        assertEquals(listOf(2L), events.map { it.id })
    }

    @Test
    fun keepsEventsInProgress() {
        val events = UpcomingEvents.build(
            listOf(raw(1, begin = "2026-10-03T21:00", end = "2026-10-03T22:00")),
            now, zone, emptySet(),
        )
        assertEquals(1, events.size)
    }

    @Test
    fun sortsByStartAndDedupesSameEventAcrossCalendars() {
        val events = UpcomingEvents.build(
            listOf(
                raw(3, title = "Late", begin = "2026-10-06T10:00", end = "2026-10-06T11:00"),
                raw(1, title = "Lecture", begin = "2026-10-05T12:30", end = "2026-10-05T13:20", calendarId = 1),
                raw(2, title = "Lecture", begin = "2026-10-05T12:30", end = "2026-10-05T13:20", calendarId = 2),
            ),
            now, zone, emptySet(),
        )
        assertEquals(listOf("Lecture", "Late"), events.map { it.title })
    }

    @Test
    fun eventColorOverridesCalendarColorAndIsOpaque() {
        val events = UpcomingEvents.build(
            listOf(
                raw(1, begin = "2026-10-05T10:00", end = "2026-10-05T11:00", eventColor = 0x00AA0000),
                raw(2, begin = "2026-10-05T11:00", end = "2026-10-05T12:00"),
            ),
            now, zone, emptySet(),
        )
        assertEquals(0xFFAA0000.toInt(), events[0].color)
        assertEquals(0xFF112233.toInt(), events[1].color)
    }

    @Test
    fun blankTitleGetsPlaceholder() {
        val events = UpcomingEvents.build(
            listOf(raw(1, title = " ", begin = "2026-10-05T10:00", end = "2026-10-05T11:00")),
            now, zone, emptySet(),
        )
        assertEquals("(No title)", events.single().title)
    }

    @Test
    fun allDayEventsAreShiftedFromUtcToLocalMidnight() {
        val utcMidnight = LocalDateTime.parse("2026-10-05T00:00").toInstant(ZoneOffset.UTC).toEpochMilli()
        val nextUtcMidnight = LocalDateTime.parse("2026-10-06T00:00").toInstant(ZoneOffset.UTC).toEpochMilli()
        val allDay = RawInstance(1, "Holiday", utcMidnight, nextUtcMidnight, true, 0, 0, 1, 0)

        val event = UpcomingEvents.build(listOf(allDay), now, zone, emptySet()).single()

        assertEquals(local("2026-10-05T00:00"), event.beginMs)
        assertEquals(local("2026-10-06T00:00"), event.endMs)
        assertTrue(event.allDay)
    }

    @Test
    fun allDayEventsSortBeforeTimedEventsOnTheSameDay() {
        val utcMidnight = LocalDateTime.parse("2026-10-05T00:00").toInstant(ZoneOffset.UTC).toEpochMilli()
        val allDay = RawInstance(1, "Holiday", utcMidnight, utcMidnight + 86_400_000, true, 0, 0, 1, 0)
        val timed = raw(2, begin = "2026-10-05T08:00", end = "2026-10-05T09:00")

        val events = UpcomingEvents.build(listOf(timed, allDay), now, zone, emptySet())

        assertEquals(listOf("Holiday", "Event 2"), events.map { it.title })
    }

    @Test
    fun respectsLimit() {
        val many = (1..20L).map {
            val start = LocalDateTime.parse("2026-10-05T00:00").plusHours(it)
            raw(it, begin = start.toString(), end = start.plusMinutes(30).toString())
        }
        assertEquals(3, UpcomingEvents.build(many, now, zone, emptySet(), limit = 3).size)
    }
}
