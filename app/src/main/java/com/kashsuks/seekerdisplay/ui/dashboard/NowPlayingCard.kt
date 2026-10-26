package com.kashsuks.seekerdisplay.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kashsuks.seekerdisplay.core.TimeFormat
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot
import com.kashsuks.seekerdisplay.ui.nowplaying.ArtworkImage
import com.kashsuks.seekerdisplay.ui.nowplaying.rememberLivePosition
import com.kashsuks.seekerdisplay.ui.theme.DashColors
import com.kashsuks.seekerdisplay.ui.theme.DashText

@Composable
fun NowPlayingCard(
    snapshot: PlaybackSnapshot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val position = rememberLivePosition(snapshot, intervalMs = 500L)
    val progress = if (snapshot.durationMs > 0) position.toFloat() / snapshot.durationMs else 0f

    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(snapshot, Modifier.size(96.dp), cornerRadius = 10.dp)
        Column(
            Modifier.padding(start = 14.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(snapshot.title, style = DashText.SongTitle, color = DashColors.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(snapshot.artist, style = DashText.SongArtist, color = DashColors.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            ProgressLine(progress, Modifier.padding(top = 10.dp))
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(TimeFormat.duration(position), style = DashText.Small, color = DashColors.Secondary)
                Text(TimeFormat.duration(snapshot.durationMs), style = DashText.Small, color = DashColors.Secondary)
            }
        }
    }
}

@Composable
private fun ProgressLine(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(DashColors.Track),
    ) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(50)).background(DashColors.Primary))
    }
}
