package com.kashsuks.seekerdisplay.data.weather

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForecastResponse(
    val current: CurrentDto,
    val hourly: HourlyDto,
    val daily: DailyDto,
)

@Serializable
data class CurrentDto(
    /** Local ISO time, e.g. "2026-10-03T21:15". */
    val time: String,
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("weather_code") val weatherCode: Int,
    @SerialName("is_day") val isDay: Int,
)

@Serializable
data class HourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("weather_code") val weatherCode: List<Int>,
    @SerialName("is_day") val isDay: List<Int>,
)

@Serializable
data class DailyDto(
    @SerialName("temperature_2m_max") val max: List<Double>,
    @SerialName("temperature_2m_min") val min: List<Double>,
)

/** Thin client for the free, keyless Open-Meteo forecast endpoint. */
class OpenMeteoApi(private val client: HttpClient) {
    suspend fun forecast(latitude: Double, longitude: Double): ForecastResponse =
        client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("current", "temperature_2m,apparent_temperature,weather_code,is_day")
            parameter("hourly", "temperature_2m,weather_code,is_day")
            parameter("daily", "temperature_2m_max,temperature_2m_min")
            parameter("timezone", "auto")
            parameter("forecast_days", 2)
        }.body()
}
