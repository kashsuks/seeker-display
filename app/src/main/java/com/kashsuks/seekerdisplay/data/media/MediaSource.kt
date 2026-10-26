package com.kashsuks.seekerdisplay.data.media

import kotlinx.coroutines.flow.Flow

/** Something that can report and control Spotify playback: this phone, or a laptop via the bridge. */
interface MediaSource {
    val type: MediaSourceType

    /** The current track, or null when nothing is available from this source. */
    val state: Flow<PlaybackSnapshot?>

    suspend fun playPause()
    suspend fun next()
    suspend fun previous()
    suspend fun seekTo(positionMs: Long)

    /** No-ops on sources that don't support shuffle/repeat. */
    suspend fun setShuffle(enabled: Boolean) {}
    suspend fun setRepeat(enabled: Boolean) {}

    /** Hint that the UI wants updates as quickly as possible (Now Playing is on screen). */
    fun setFastPolling(enabled: Boolean) {}
}
