package com.kashsuks.seekerdisplay.data.calendar

data class CalendarEvent(
    val id: Long,
    val title: String,
    val beginMs: Long,
    val endMs: Long,
    val allDay: Boolean,
    /** ARGB colour of the calendar (or event) this came from. */
    val color: Int,
    val calendarId: Long,
)
