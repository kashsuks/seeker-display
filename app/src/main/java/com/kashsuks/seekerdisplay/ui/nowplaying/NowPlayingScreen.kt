package com.kashsuks.seekerdisplay.ui.nowplaying

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kashsuks.seekerdisplay.core.TimeFormat
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot
import com.kashsuks.seekerdisplay.ui.theme.DashText

/** Fullscreen player. Tapping anywhere that isn't a control returns to the dashboard. */
@Composable
fun NowPlayingScreen(
    snapshot: PlaybackSnapshot,
    actions: PlaybackActions,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors by rememberBackgroundColors(snapshot)
    val textColor = Color(colors.onBackground)

    Box(
        modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        PaletteBackground(colors, Modifier.fillMaxSize())

        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 40.dp, vertical = 24.dp)) {
            val artSize = minOf(maxHeight, maxWidth * 0.4f)
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                ArtworkImage(
                    snapshot,
                    Modifier.size(artSize).shadow(24.dp, RoundedCornerShape(16.dp)),
                    cornerRadius = 16.dp,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        snapshot.title,
                        style = DashText.SongTitle.copy(fontSize = 34.sp),
                        color = textColor,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().basicMarquee(),
                    )
                    Text(snapshot.artist, style = DashText.SongArtist.copy(fontSize = 22.sp), color = textColor.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(snapshot.sourceLabel, style = DashText.Small, color = textColor.copy(alpha = 0.55f), modifier = Modifier.padding(top = 4.dp))
                    Spacer(Modifier.height(20.dp))
                    SeekBar(snapshot, actions.onSeek, textColor)
                    Spacer(Modifier.height(8.dp))
                    PlaybackControls(snapshot, actions, textColor, Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Composable
private fun SeekBar(snapshot: PlaybackSnapshot, onSeek: (Long) -> Unit, tint: Color) {
    val livePosition = rememberLivePosition(snapshot, intervalMs = 100L)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val duration = snapshot.durationMs.coerceAtLeast(1)
    val fraction = dragFraction ?: (livePosition.toFloat() / duration).coerceIn(0f, 1f)
    val shownPosition = (fraction * duration).toLong()

    Column {
        Slider(
            value = fraction,
            onValueChange = { dragFraction = it },
            onValueChangeFinished = {
                dragFraction?.let { onSeek((it * duration).toLong()) }
                dragFraction = null
            },
            enabled = snapshot.durationMs > 0,
            colors = SliderDefaults.colors(
                thumbColor = tint,
                activeTrackColor = tint,
                inactiveTrackColor = tint.copy(alpha = 0.25f),
                disabledThumbColor = tint.copy(alpha = 0.5f),
                disabledActiveTrackColor = tint.copy(alpha = 0.5f),
                disabledInactiveTrackColor = tint.copy(alpha = 0.2f),
            ),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(TimeFormat.duration(shownPosition), style = DashText.Small, color = tint.copy(alpha = 0.7f))
            Text("-" + TimeFormat.duration((duration - shownPosition).coerceAtLeast(0)), style = DashText.Small, color = tint.copy(alpha = 0.7f))
        }
    }
}
