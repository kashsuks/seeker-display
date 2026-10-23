package com.kashsuks.seekerdisplay

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kashsuks.seekerdisplay.core.TimeTicker
import com.kashsuks.seekerdisplay.ui.dashboard.DashboardScreen
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
                val now by produceState(ZonedDateTime.now()) {
                    TimeTicker.ticks(context).collect { value = it }
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
                DashboardScreen(state, onTimeClick = {}, onMediaClick = {})
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
