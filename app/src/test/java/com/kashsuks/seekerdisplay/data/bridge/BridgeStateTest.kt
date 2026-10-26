package com.kashsuks.seekerdisplay.data.bridge

import com.kashsuks.seekerdisplay.data.media.MediaSourceType
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeStateTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parsesBridgeResponseAndIgnoresExtraFields() {
        val dto = json.decodeFromString<BridgeStateDto>(
            """{"running":true,"isPlaying":true,"title":"Nights","artist":"Frank Ocean","album":"Blonde",
               "trackId":"x","artworkUrl":"https://i.scdn.co/image/a","durationMs":307000,"positionMs":102500,
               "shuffle":true,"repeat":false,"host":"MacBook","serverTimeMs":123}""",
        )
        val snapshot = dto.toSnapshot(capturedAtElapsedRealtime = 5_000)!!

        assertEquals(MediaSourceType.MAC, snapshot.source)
        assertEquals("On MacBook", snapshot.sourceLabel)
        assertEquals("Nights", snapshot.title)
        assertEquals("https://i.scdn.co/image/a", snapshot.artworkUrl)
        assertEquals(307_000, snapshot.durationMs)
        assertEquals(102_500, snapshot.positionMs)
        assertEquals(5_000, snapshot.capturedAtElapsedRealtime)
        assertTrue(snapshot.supportsShuffleRepeat)
        assertTrue(snapshot.shuffle)
        assertFalse(snapshot.repeat)
    }

    @Test
    fun spotifyNotRunningOrNoTrackGivesNoSnapshot() {
        assertNull(BridgeStateDto(running = false, title = "x").toSnapshot(0))
        assertNull(BridgeStateDto(running = true, title = "").toSnapshot(0))
    }

    @Test
    fun missingArtworkAndHostFallBackGracefully() {
        val snapshot = BridgeStateDto(running = true, title = "t", artworkUrl = "").toSnapshot(0)!!
        assertNull(snapshot.artworkUrl)
        assertEquals("On Mac", snapshot.sourceLabel)
    }

    @Test
    fun positionInterpolatesWhilePlayingAndFreezesWhenPaused() {
        val playing = BridgeStateDto(running = true, title = "t", isPlaying = true, durationMs = 10_000, positionMs = 1_000).toSnapshot(100)!!
        assertEquals(3_000, playing.positionAt(2_100))
        val paused = BridgeStateDto(running = true, title = "t", isPlaying = false, durationMs = 10_000, positionMs = 1_000).toSnapshot(100)!!
        assertEquals(1_000, paused.positionAt(2_100))
    }
}
