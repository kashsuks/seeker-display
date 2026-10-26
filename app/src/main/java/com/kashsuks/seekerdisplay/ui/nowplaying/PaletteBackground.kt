package com.kashsuks.seekerdisplay.ui.nowplaying

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Gradient colours for the current cover, recomputed only when the track changes. */
@Composable
fun rememberBackgroundColors(snapshot: PlaybackSnapshot): State<BackgroundColors> {
    val context = LocalContext.current
    return produceState(BackgroundColorsFactory.Default, snapshot.trackKey, snapshot.artwork, snapshot.artworkUrl) {
        val bitmap = snapshot.artwork ?: snapshot.artworkUrl?.let { url ->
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
            (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
        }
        value = bitmap?.let { withContext(Dispatchers.Default) { extract(it) } } ?: BackgroundColorsFactory.Default
    }
}

private fun extract(bitmap: Bitmap): BackgroundColors {
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    return BackgroundColorsFactory.derive(
        dominant = palette.dominantSwatch?.rgb,
        vibrant = palette.vibrantSwatch?.rgb ?: palette.lightVibrantSwatch?.rgb,
        muted = palette.mutedSwatch?.rgb,
        darkMuted = palette.darkMutedSwatch?.rgb ?: palette.darkVibrantSwatch?.rgb,
    )
}

/** A slowly drifting two-colour gradient that fades to new colours when the track changes. */
@Composable
fun PaletteBackground(colors: BackgroundColors, modifier: Modifier = Modifier) {
    val start by animateColorAsState(Color(colors.start), tween(900), label = "bgStart")
    val end by animateColorAsState(Color(colors.end), tween(900), label = "bgEnd")
    val drift by rememberInfiniteTransition(label = "drift").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "driftValue",
    )
    Box(
        modifier.drawBehind {
            val shift = size.width * 0.35f * drift
            drawRect(
                Brush.linearGradient(
                    colors = listOf(start, end),
                    start = Offset(shift, 0f),
                    end = Offset(size.width - shift, size.height),
                ),
            )
        },
    )
}
