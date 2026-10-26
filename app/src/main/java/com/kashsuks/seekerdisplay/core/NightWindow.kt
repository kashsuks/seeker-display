package com.kashsuks.seekerdisplay.core

/** A daily time range that may run past midnight, e.g. 23:00 to 07:00. Values are minutes after midnight. */
object NightWindow {
    const val MINUTES_PER_DAY = 24 * 60

    fun isActive(nowMinute: Int, startMinute: Int, endMinute: Int): Boolean = when {
        startMinute == endMinute -> false
        startMinute < endMinute -> nowMinute in startMinute until endMinute
        else -> nowMinute >= startMinute || nowMinute < endMinute
    }

    /** Moves a time by whole hours, wrapping around the day. */
    fun shiftHours(minute: Int, hours: Int): Int =
        ((minute + hours * 60) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY

    fun format(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)
}
