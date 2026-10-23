package com.kashsuks.seekerdisplay.data.weather

import kotlinx.serialization.Serializable

@Serializable
enum class WeatherCondition {
    CLEAR,
    MOSTLY_CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    SNOW,
    THUNDER,
}

/** A condition plus whether it is daytime, which picks the sun or moon variant of the icon. */
@Serializable
data class WeatherSymbol(val condition: WeatherCondition, val isDay: Boolean)

@Serializable
data class HourlySlot(
    /** Hour of day (0-23), or null for the "Now" slot. */
    val hour: Int?,
    val tempC: Int,
    val symbol: WeatherSymbol,
)

@Serializable
data class WeatherData(
    val city: String,
    val tempC: Int,
    val feelsLikeC: Int,
    val highC: Int,
    val lowC: Int,
    val symbol: WeatherSymbol,
    val label: String,
    val hourly: List<HourlySlot>,
    val isStale: Boolean = false,
)
