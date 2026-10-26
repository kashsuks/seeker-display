package com.kashsuks.seekerdisplay.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kashsuks.seekerdisplay.core.TimeFormat
import com.kashsuks.seekerdisplay.data.weather.HourlySlot
import com.kashsuks.seekerdisplay.data.weather.WeatherData
import com.kashsuks.seekerdisplay.ui.theme.DashColors
import com.kashsuks.seekerdisplay.ui.theme.DashText

@Composable
fun WeatherPanel(weather: WeatherData?, modifier: Modifier = Modifier) {
    if (weather == null) {
        Column(modifier) {
            Text("WEATHER", style = DashText.City, color = DashColors.Blue)
            Text("--°", style = DashText.Temp, color = DashColors.Secondary)
        }
        return
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    // A trailing dot means the forecast is from the cache because a refresh failed.
                    text = weather.city.uppercase() + if (weather.isStale) " •" else "",
                    style = DashText.City,
                    color = DashColors.Blue,
                )
                Text("${weather.tempC}°", style = DashText.Temp, color = DashColors.Primary)
            }
            Column(horizontalAlignment = Alignment.End) {
                WeatherIcon(weather.symbol, size = 36.dp)
                Text(weather.label, style = DashText.SongTitle, color = DashColors.Primary)
                Text("H:${weather.highC}°  L:${weather.lowC}°", style = DashText.WeatherDetail, color = DashColors.Primary)
                Text("Feels Like: ${weather.feelsLikeC}°", style = DashText.WeatherDetail, color = DashColors.Primary)
            }
        }
        HourlyRow(weather.hourly, Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun HourlyRow(slots: List<HourlySlot>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        slots.forEach { slot ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = slot.hour?.let(TimeFormat::hourOfDay) ?: "Now",
                    style = DashText.HourLabel,
                    color = DashColors.Secondary,
                    textAlign = TextAlign.Center,
                )
                WeatherIcon(slot.symbol, size = 24.dp, modifier = Modifier.padding(vertical = 6.dp))
                Text("${slot.tempC}°", style = DashText.HourTemp, color = DashColors.Primary)
            }
        }
    }
}
