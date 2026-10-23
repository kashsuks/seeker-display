package com.kashsuks.seekerdisplay.data.media

import android.graphics.Bitmap
import android.os.SystemClock

enum class MediaSourceType { PHONE, MAC }

data class PlaybackSnapshot(
    val source: MediaSourceType,
    val sourceLabel: String,
    val isPlaying: Boolean,
    val title: String,
    val artist: String,
    val album: String = "",
    val artwork: Bitmap? = null,
    val artworkUrl: String? = null,
    val durationMs: Long,
    /** Position at [capturedAtElapsedRealtime]. */
    val positionMs: Long,
    val capturedAtElapsedRealtime: Long = SystemClock.elapsedRealtime(),
    val supportsShuffleRepeat: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
) {
    /** Interpolated position so the progress bar moves between polls. */
    fun positionAt(elapsedRealtime: Long): Long {
        val raw = if (isPlaying) positionMs + (elapsedRealtime - capturedAtElapsedRealtime) else positionMs
        return if (durationMs > 0) raw.coerceIn(0, durationMs) else raw.coerceAtLeast(0)
    }

    /** Identifies the track so UI can tell a track change from a progress update. */
    val trackKey: String get() = "$title|$artist|$album"
}
