package com.kashsuks.seekerdisplay.data.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class WeatherMapperTest {
    @Test
    fun slotsStartAtNextEvenHourAndStepByTwo() {
        val slots = WeatherMapper.slotTimes(LocalDateTime.parse("2026-10-03T21:24"))
        assertEquals(listOf(22, 0, 2, 4, 6, 8), slots.map { it.hour })
        assertEquals(LocalDateTime.parse("2026-10-04T00:00"), slots[1])
    }

    @Test
    fun slotsSkipTheCurrentEvenHour() {
        val slots = WeatherMapper.slotTimes(LocalDateTime.parse("2026-10-03T22:10"))
        assertEquals(listOf(0, 2, 4, 6, 8, 10), slots.map { it.hour })
    }

    @Test
    fun mapBuildsNowSlotPlusSixForecastSlots() {
        val times = (0 until 48).map { LocalDateTime.parse("2026-10-03T00:00").plusHours(it.toLong()).toString().take(16) }
        val response = ForecastResponse(
            current = CurrentDto("2026-10-03T21:15", 9.4, 5.6, 0, 0),
            hourly = HourlyDto(
                time = times,
                temperature = List(48) { it.toDouble() },
                weatherCode = List(48) { 3 },
                isDay = List(48) { 0 },
            ),
            daily = DailyDto(max = listOf(16.2, 14.0), min = listOf(4.6, 3.0)),
        )

        val data = WeatherMapper.map(response, "Waterloo")

        assertEquals(9, data.tempC)
        assertEquals(6, data.feelsLikeC)
        assertEquals(16, data.highC)
        assertEquals(5, data.lowC)
        assertEquals("Clear", data.label)
        assertEquals(7, data.hourly.size)
        assertNull(data.hourly.first().hour)
        assertEquals(listOf(22, 0, 2, 4, 6, 8), data.hourly.drop(1).map { it.hour })
        assertEquals(22, data.hourly[1].tempC)
    }
}
