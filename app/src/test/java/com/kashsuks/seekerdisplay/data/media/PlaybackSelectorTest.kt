package com.kashsuks.seekerdisplay.data.media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackSelectorTest {
    private class FakeSource(override val type: MediaSourceType) : MediaSource {
        override val state: Flow<PlaybackSnapshot?> = emptyFlow()
        override suspend fun playPause() {}
        override suspend fun next() {}
        override suspend fun previous() {}
        override suspend fun seekTo(positionMs: Long) {}
    }

    private val phone = FakeSource(MediaSourceType.PHONE)
    private val mac = FakeSource(MediaSourceType.MAC)

    private fun candidate(source: MediaSource, playing: Boolean, changedAt: Long) = PlaybackCandidate(
        source,
        PlaybackSnapshot(
            source = source.type, sourceLabel = "", isPlaying = playing, title = "t", artist = "a",
            durationMs = 1000, positionMs = 0, capturedAtElapsedRealtime = 0,
        ),
        changedAt,
    )

    @Test
    fun nothingAvailableSelectsNothing() {
        assertNull(PlaybackSelector.select(emptyList(), 0))
    }

    @Test
    fun playingSourceBeatsPausedSource() {
        val chosen = PlaybackSelector.select(listOf(candidate(phone, false, 900), candidate(mac, true, 100)), nowMs = 1000)
        assertEquals(MediaSourceType.MAC, chosen?.source?.type)
    }

    @Test
    fun mostRecentlyChangedWinsWhenBothPlaying() {
        val chosen = PlaybackSelector.select(listOf(candidate(phone, true, 500), candidate(mac, true, 800)), nowMs = 1000)
        assertEquals(MediaSourceType.MAC, chosen?.source?.type)
    }

    @Test
    fun pausedTrackStaysVisibleForTenMinutesThenHides() {
        val paused = candidate(phone, false, changedAt = 0)
        assertEquals(phone, PlaybackSelector.select(listOf(paused), nowMs = PlaybackSelector.PAUSED_VISIBLE_MS)?.source)
        assertNull(PlaybackSelector.select(listOf(paused), nowMs = PlaybackSelector.PAUSED_VISIBLE_MS + 1))
    }
}
