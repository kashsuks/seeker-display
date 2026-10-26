package com.kashsuks.seekerdisplay.ui.nowplaying

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot
import kotlinx.coroutines.delay

/** The track position, advanced locally every [intervalMs] so progress moves smoothly between polls. */
@Composable
fun rememberLivePosition(snapshot: PlaybackSnapshot, intervalMs: Long = 250L): Long {
    var position by remember(snapshot) { mutableLongStateOf(snapshot.positionAt(SystemClock.elapsedRealtime())) }
    LaunchedEffect(snapshot) {
        position = snapshot.positionAt(SystemClock.elapsedRealtime())
        while (snapshot.isPlaying) {
            delay(intervalMs)
            position = snapshot.positionAt(SystemClock.elapsedRealtime())
        }
    }
    return position
}
