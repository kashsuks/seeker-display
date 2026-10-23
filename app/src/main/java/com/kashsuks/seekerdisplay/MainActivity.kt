package com.kashsuks.seekerdisplay

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kashsuks.seekerdisplay.core.TimeTicker
import com.kashsuks.seekerdisplay.ui.AppRoot
import com.kashsuks.seekerdisplay.ui.Screen
import com.kashsuks.seekerdisplay.ui.dashboard.DashboardState
import com.kashsuks.seekerdisplay.ui.preview.PlaceholderData
import com.kashsuks.seekerdisplay.ui.theme.SeekerDisplayTheme
import java.time.ZonedDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()
        setContent {
            SeekerDisplayTheme {
                val context = LocalContext.current
                val showSeconds = false
                var screen by remember { mutableStateOf(Screen.Dashboard) }
                // Seconds only tick while the focus clock is on screen, to save battery elsewhere.
                val tickMs = if (screen == Screen.FocusClock && showSeconds) 1_000L else 60_000L
                val now by produceState(ZonedDateTime.now(), tickMs) {
                    TimeTicker.ticks(context, tickMs).collect { value = it }
                }
                val state = remember(now) {
                    DashboardState(
                        now = now,
                        weather = PlaceholderData.weather,
                        events = PlaceholderData.events(now),
                        calendarPermission = true,
                        playback = PlaceholderData.playback,
                    )
                }
                AppRoot(state, showSeconds, onScreenChanged = { screen = it })
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
