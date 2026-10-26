package com.kashsuks.seekerdisplay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

object DashColors {
    val Black = Color(0xFF000000)
    val Red = Color(0xFFFF453A)
    val Blue = Color(0xFF64D2FF)
    val Time = Color(0xFFD6DEFF)
    val Primary = Color(0xFFFFFFFF)
    val Secondary = Color(0xFF9A9AA2)
    val Sun = Color(0xFFFFD60A)
    val Moon = Color(0xFFE5E5F0)
    val Cloud = Color(0xFFE0E0E8)
    val Rain = Color(0xFF64B5FF)
    val Track = Color(0x33FFFFFF)
}

/** The app's rounded display face. Falls back to the system default if rounded isn't available. */
val DisplayFont: FontFamily = FontFamily.SansSerif

object DashText {
    val Weekday = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = 0.02.em)
    val Time = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 128.sp, letterSpacing = (-0.03).em, lineHeight = 1.0.em)
    val Date = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 24.sp)
    val City = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    val Temp = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 76.sp, lineHeight = 1.0.em)
    val WeatherDetail = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    val HourLabel = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    val HourTemp = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    val EventHero = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 26.sp)
    val EventSub = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    val EventRow = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    val SongTitle = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    val SongArtist = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 16.sp)
    val Small = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 12.sp)
}

@Composable
fun SeekerDisplayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = DashColors.Black, surface = DashColors.Black, onBackground = DashColors.Primary, onSurface = DashColors.Primary),
        content = content,
    )
}
