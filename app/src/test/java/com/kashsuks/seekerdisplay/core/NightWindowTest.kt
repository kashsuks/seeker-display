package com.kashsuks.seekerdisplay.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightWindowTest {
    private val start = 23 * 60
    private val end = 7 * 60

    @Test
    fun overnightWindowCoversLateEveningAndEarlyMorning() {
        assertTrue(NightWindow.isActive(23 * 60, start, end))
        assertTrue(NightWindow.isActive(0, start, end))
        assertTrue(NightWindow.isActive(6 * 60 + 59, start, end))
    }

    @Test
    fun overnightWindowExcludesDaytime() {
        assertFalse(NightWindow.isActive(7 * 60, start, end))
        assertFalse(NightWindow.isActive(12 * 60, start, end))
        assertFalse(NightWindow.isActive(22 * 60 + 59, start, end))
    }

    @Test
    fun sameDayWindow() {
        assertTrue(NightWindow.isActive(13 * 60, 13 * 60, 15 * 60))
        assertFalse(NightWindow.isActive(15 * 60, 13 * 60, 15 * 60))
    }

    @Test
    fun emptyWindowNeverActive() {
        assertFalse(NightWindow.isActive(0, 600, 600))
    }

    @Test
    fun shiftHoursWrapsBothWays() {
        assertEquals(60, NightWindow.shiftHours(23 * 60, 2))
        assertEquals(23 * 60, NightWindow.shiftHours(60, -2))
    }

    @Test
    fun formatsAs24Hour() {
        assertEquals("07:00", NightWindow.format(7 * 60))
        assertEquals("23:30", NightWindow.format(23 * 60 + 30))
    }
}
