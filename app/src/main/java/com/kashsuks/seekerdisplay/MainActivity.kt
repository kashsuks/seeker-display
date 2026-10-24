package com.kashsuks.seekerdisplay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kashsuks.seekerdisplay.core.TimeTicker
import com.kashsuks.seekerdisplay.ui.AppRoot
import com.kashsuks.seekerdisplay.ui.DashboardViewModel
import com.kashsuks.seekerdisplay.ui.Screen
import com.kashsuks.seekerdisplay.ui.dashboard.DashboardState
import com.kashsuks.seekerdisplay.ui.preview.PlaceholderData
import com.kashsuks.seekerdisplay.ui.settings.SettingsActions
import com.kashsuks.seekerdisplay.ui.settings.SettingsState
import com.kashsuks.seekerdisplay.ui.theme.SeekerDisplayTheme
import java.time.ZonedDateTime

class MainActivity : ComponentActivity() {
    private val viewModel: DashboardViewModel by viewModels {
        viewModelFactory { initializer { DashboardViewModel((application as SeekerApp).container) } }
    }

    /** Bumped whenever permissions may have changed so Compose re-reads their status. */
    private var permissionVersion by mutableIntStateOf(0)

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionVersion++
        viewModel.onPermissionsChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMissingPermissions()
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
                val weather by viewModel.weather.collectAsStateWithLifecycle()
                val events by viewModel.events.collectAsStateWithLifecycle()
                val calendarPermission by viewModel.calendarPermission.collectAsStateWithLifecycle()
                val state = remember(now, weather, events, calendarPermission) {
                    DashboardState(
                        now = now,
                        weather = weather,
                        events = events,
                        calendarPermission = calendarPermission,
                        playback = PlaceholderData.playback,
                    )
                }
                val calendars by viewModel.calendars.collectAsStateWithLifecycle()
                val hiddenCalendars by viewModel.hiddenCalendarIds.collectAsStateWithLifecycle()
                val settingsState = SettingsState(
                    locationGranted = isGranted(Manifest.permission.ACCESS_COARSE_LOCATION, permissionVersion),
                    calendarGranted = calendarPermission,
                    calendars = calendars,
                    hiddenCalendarIds = hiddenCalendars,
                )
                val settingsActions = remember {
                    SettingsActions(
                        onGrantLocation = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)) },
                        onGrantCalendar = { permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR)) },
                        onToggleCalendar = viewModel::setCalendarVisible,
                        onOpenAppSettings = ::openAppSettings,
                    )
                }
                AppRoot(
                    state = state,
                    showSeconds = showSeconds,
                    settingsState = settingsState,
                    settingsActions = settingsActions,
                    onScreenChanged = {
                        screen = it
                        if (it == Screen.Settings) viewModel.loadCalendars()
                    },
                )
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun isGranted(permission: String, version: Int): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }

    private fun requestMissingPermissions() {
        val wanted = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_CALENDAR)
        val missing = wanted.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
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
