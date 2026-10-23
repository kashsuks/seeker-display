package com.kashsuks.seekerdisplay.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kashsuks.seekerdisplay.ui.theme.DashColors

@Composable
fun DashboardScreen(
    state: DashboardState,
    onTimeClick: () -> Unit,
    onMediaClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxSize()
            .background(DashColors.Black)
            .safeDrawingPadding()
            .padding(horizontal = 36.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(
            Modifier.weight(1f).fillMaxSize(),
            verticalArrangement = if (state.playback == null) Arrangement.Center else Arrangement.SpaceBetween,
        ) {
            ClockPanel(state.now, onTimeClick, Modifier.animateContentSize())
            state.playback?.let { playback ->
                NowPlayingCard(playback, onMediaClick, Modifier.fillMaxWidth().height(96.dp))
            }
        }
        Column(
            Modifier.weight(1f).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            WeatherPanel(state.weather, Modifier.fillMaxWidth())
            UpNextPanel(state.events, state.now, state.calendarPermission, Modifier.fillMaxWidth())
        }
    }
}
