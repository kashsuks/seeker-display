package com.kashsuks.seekerdisplay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kashsuks.seekerdisplay.ui.dashboard.DashboardScreen
import com.kashsuks.seekerdisplay.ui.dashboard.DashboardState
import com.kashsuks.seekerdisplay.ui.focusclock.FocusClockScreen
import com.kashsuks.seekerdisplay.ui.settings.SettingsActions
import com.kashsuks.seekerdisplay.ui.settings.SettingsScreen
import com.kashsuks.seekerdisplay.ui.settings.SettingsState

enum class Screen { Dashboard, FocusClock, Settings }

@Composable
fun AppRoot(
    state: DashboardState,
    showSeconds: Boolean,
    settingsState: SettingsState,
    settingsActions: SettingsActions,
    onScreenChanged: (Screen) -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.Dashboard) }

    fun go(target: Screen) {
        screen = target
        onScreenChanged(target)
    }

    BackHandler(enabled = screen != Screen.Dashboard) { go(Screen.Dashboard) }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            (fadeIn(tween(350)) + scaleIn(tween(350), initialScale = 0.94f)) togetherWith
                (fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 1.04f))
        },
        label = "screen",
    ) { target ->
        when (target) {
            Screen.Dashboard -> DashboardScreen(
                state = state,
                onTimeClick = { go(Screen.FocusClock) },
                onMediaClick = {},
                onLongPress = { go(Screen.Settings) },
            )
            Screen.Settings -> SettingsScreen(
                state = settingsState,
                actions = settingsActions,
                onBack = { go(Screen.Dashboard) },
            )
            Screen.FocusClock -> FocusClockScreen(
                now = state.now,
                showSeconds = showSeconds,
                onDismiss = { go(Screen.Dashboard) },
            )
        }
    }
}
