package com.kashsuks.seekerdisplay.data.bridge

import com.kashsuks.seekerdisplay.data.media.MediaSourceType
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot
import kotlinx.serialization.Serializable

/** Response of the bridge's `GET /state`. */
@Serializable
data class BridgeStateDto(
    val running: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val trackId: String = "",
    val artworkUrl: String = "",
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val shuffle: Boolean = false,
    val repeat: Boolean = false,
    val host: String = "",
)

/** Null when Spotify isn't running or has no current track. */
fun BridgeStateDto.toSnapshot(capturedAtElapsedRealtime: Long): PlaybackSnapshot? {
    if (!running || title.isBlank()) return null
    return PlaybackSnapshot(
        source = MediaSourceType.MAC,
        sourceLabel = if (host.isBlank()) "On Mac" else "On $host",
        isPlaying = isPlaying,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl.takeIf { it.startsWith("http") },
        durationMs = durationMs.coerceAtLeast(0),
        positionMs = positionMs.coerceAtLeast(0),
        capturedAtElapsedRealtime = capturedAtElapsedRealtime,
        supportsShuffleRepeat = true,
        shuffle = shuffle,
        repeat = repeat,
    )
}
