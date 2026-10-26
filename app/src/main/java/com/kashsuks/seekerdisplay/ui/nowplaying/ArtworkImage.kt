package com.kashsuks.seekerdisplay.ui.nowplaying

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot

@Composable
fun ArtworkImage(
    snapshot: PlaybackSnapshot,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
) {
    Box(
        modifier.clip(RoundedCornerShape(cornerRadius)).background(Color(0xFF2C2C2E)),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = snapshot.artwork
        when {
            bitmap != null -> Image(bitmap.asImageBitmap(), contentDescription = snapshot.album, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            snapshot.artworkUrl != null -> AsyncImage(snapshot.artworkUrl, contentDescription = snapshot.album, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else -> Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color(0xFF8E8E93))
        }
    }
}
