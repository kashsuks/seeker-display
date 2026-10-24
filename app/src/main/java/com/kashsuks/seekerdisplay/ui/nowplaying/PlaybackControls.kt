package com.kashsuks.seekerdisplay.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot

class PlaybackActions(
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeek: (positionMs: Long) -> Unit,
    val onShuffle: (enabled: Boolean) -> Unit,
    val onRepeat: (enabled: Boolean) -> Unit,
)

@Composable
fun PlaybackControls(
    snapshot: PlaybackSnapshot,
    actions: PlaybackActions,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (snapshot.supportsShuffleRepeat) {
            ToggleButton(Icons.Rounded.Shuffle, "Shuffle", snapshot.shuffle, tint) { actions.onShuffle(!snapshot.shuffle) }
        }
        ControlButton(Icons.Rounded.SkipPrevious, "Previous", tint, size = 48.dp, onClick = actions.onPrevious)
        // The play/pause button is a filled disc in the text colour, with the icon cut out in the background colour.
        IconButton(
            onClick = actions.onPlayPause,
            modifier = Modifier.size(72.dp).clip(CircleShape).background(tint),
        ) {
            Icon(
                imageVector = if (snapshot.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (snapshot.isPlaying) "Pause" else "Play",
                tint = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier.size(40.dp),
            )
        }
        ControlButton(Icons.Rounded.SkipNext, "Next", tint, size = 48.dp, onClick = actions.onNext)
        if (snapshot.supportsShuffleRepeat) {
            ToggleButton(Icons.Rounded.Repeat, "Repeat", snapshot.repeat, tint) { actions.onRepeat(!snapshot.repeat) }
        }
    }
}

@Composable
private fun ControlButton(icon: ImageVector, description: String, tint: Color, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(size + 8.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size))
    }
}

@Composable
private fun ToggleButton(icon: ImageVector, description: String, enabled: Boolean, tint: Color, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(26.dp),
        )
    }
}
