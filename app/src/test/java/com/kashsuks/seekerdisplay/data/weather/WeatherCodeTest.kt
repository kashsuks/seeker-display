package com.kashsuks.seekerdisplay.data.weather

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeTest {
    @Test
    fun mapsCommonCodes() {
        assertEquals(WeatherCondition.CLEAR, WeatherCode.condition(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCode.condition(2))
        assertEquals(WeatherCondition.FOG, WeatherCode.condition(45))
        assertEquals(WeatherCondition.DRIZZLE, WeatherCode.condition(53))
        assertEquals(WeatherCondition.RAIN, WeatherCode.condition(63))
        assertEquals(WeatherCondition.RAIN, WeatherCode.condition(81))
        assertEquals(WeatherCondition.SNOW, WeatherCode.condition(73))
        assertEquals(WeatherCondition.SNOW, WeatherCode.condition(86))
        assertEquals(WeatherCondition.THUNDER, WeatherCode.condition(95))
    }

    @Test
    fun unknownCodeFallsBackToCloudy() {
        assertEquals(WeatherCondition.CLOUDY, WeatherCode.condition(1234))
    }

    @Test
    fun symbolCarriesDayFlag() {
        assertEquals(WeatherSymbol(WeatherCondition.CLEAR, isDay = false), WeatherCode.symbol(0, isDay = false))
    }

    @Test
    fun labelsReadNaturally() {
        assertEquals("Clear", WeatherCode.label(0))
        assertEquals("Thunderstorm", WeatherCode.label(95))
    }
}
