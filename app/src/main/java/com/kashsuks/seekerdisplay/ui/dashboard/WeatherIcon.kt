package com.kashsuks.seekerdisplay.ui.dashboard

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.FilterDrama
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kashsuks.seekerdisplay.data.weather.WeatherCondition
import com.kashsuks.seekerdisplay.data.weather.WeatherSymbol
import com.kashsuks.seekerdisplay.ui.theme.DashColors

internal fun WeatherSymbol.icon(): ImageVector = when (condition) {
    WeatherCondition.CLEAR -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    WeatherCondition.MOSTLY_CLEAR -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    WeatherCondition.PARTLY_CLOUDY -> Icons.Rounded.FilterDrama
    WeatherCondition.CLOUDY -> Icons.Rounded.Cloud
    WeatherCondition.FOG -> Icons.Rounded.BlurOn
    WeatherCondition.DRIZZLE -> Icons.Rounded.WaterDrop
    WeatherCondition.RAIN -> Icons.Rounded.Umbrella
    WeatherCondition.SNOW -> Icons.Rounded.AcUnit
    WeatherCondition.THUNDER -> Icons.Rounded.Thunderstorm
}

internal fun WeatherSymbol.tint(): Color = when (condition) {
    WeatherCondition.CLEAR, WeatherCondition.MOSTLY_CLEAR -> if (isDay) DashColors.Sun else DashColors.Moon
    WeatherCondition.PARTLY_CLOUDY -> if (isDay) DashColors.Sun else DashColors.Moon
    WeatherCondition.CLOUDY, WeatherCondition.FOG -> DashColors.Cloud
    WeatherCondition.DRIZZLE, WeatherCondition.RAIN -> DashColors.Rain
    WeatherCondition.SNOW -> Color.White
    WeatherCondition.THUNDER -> DashColors.Sun
}

@Composable
fun WeatherIcon(symbol: WeatherSymbol, size: Dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector = symbol.icon(),
        contentDescription = symbol.condition.name,
        tint = symbol.tint(),
        modifier = modifier.size(size),
    )
}
