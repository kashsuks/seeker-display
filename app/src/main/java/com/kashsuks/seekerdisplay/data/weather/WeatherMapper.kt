package com.kashsuks.seekerdisplay.data.weather

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** Turns an Open-Meteo response into the model the dashboard draws. */
object WeatherMapper {
    private const val FORECAST_SLOTS = 6
    private const val SLOT_STEP_HOURS = 2L

    /**
     * The hours shown after "Now": the next even hour strictly after [now], then every two hours.
     * At 21:24 this is 22, 00, 02, 04, 06, 08.
     */
    fun slotTimes(now: LocalDateTime): List<LocalDateTime> {
        val thisHour = now.truncatedTo(ChronoUnit.HOURS)
        val first = if (thisHour.hour % 2 == 0) thisHour.plusHours(2) else thisHour.plusHours(1)
        return List(FORECAST_SLOTS) { first.plusHours(it * SLOT_STEP_HOURS) }
    }

    fun map(response: ForecastResponse, city: String): WeatherData {
        val now = LocalDateTime.parse(response.current.time)
        val current = response.current
        val currentSymbol = WeatherCode.symbol(current.weatherCode, current.isDay == 1)

        val hourlyByTime = response.hourly.time.mapIndexed { i, t ->
            LocalDateTime.parse(t) to HourlySlot(
                hour = LocalDateTime.parse(t).hour,
                tempC = response.hourly.temperature[i].roundToInt(),
                symbol = WeatherCode.symbol(response.hourly.weatherCode[i], response.hourly.isDay[i] == 1),
            )
        }.toMap()

        val slots = buildList {
            add(HourlySlot(hour = null, tempC = current.temperature.roundToInt(), symbol = currentSymbol))
            slotTimes(now).mapNotNullTo(this) { hourlyByTime[it] }
        }

        return WeatherData(
            city = city,
            tempC = current.temperature.roundToInt(),
            feelsLikeC = current.apparentTemperature.roundToInt(),
            highC = response.daily.max.firstOrNull()?.roundToInt() ?: current.temperature.roundToInt(),
            lowC = response.daily.min.firstOrNull()?.roundToInt() ?: current.temperature.roundToInt(),
            symbol = currentSymbol,
            label = WeatherCode.label(current.weatherCode),
            hourly = slots,
        )
    }
}
