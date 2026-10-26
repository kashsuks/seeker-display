package com.kashsuks.seekerdisplay.data.media

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.scan

/** What the UI shows, plus the source that owns it so controls go to the right place. */
data class ActivePlayback(val snapshot: PlaybackSnapshot, val source: MediaSource)

/** Merges every [MediaSource] into the single track the dashboard should display. */
class PlaybackRepository(private val sources: List<MediaSource>) {

    val active: Flow<ActivePlayback?> = combine(
        sources.map { track(it) } + clock(),
    ) { values ->
        val candidates = values.filterIsInstance<PlaybackCandidate>()
        PlaybackSelector.select(candidates, System.currentTimeMillis())?.let { ActivePlayback(it.snapshot, it.source) }
    }.distinctUntilChanged()

    fun setFastPolling(enabled: Boolean) {
        sources.forEach { it.setFastPolling(enabled) }
    }

    /** Remembers when each source last started, stopped or changed track. */
    private fun track(source: MediaSource): Flow<PlaybackCandidate?> =
        source.state.scan(null as PlaybackCandidate?) { previous, snapshot ->
            when {
                snapshot == null -> null
                previous == null ||
                    previous.snapshot.isPlaying != snapshot.isPlaying ||
                    previous.snapshot.trackKey != snapshot.trackKey ->
                    PlaybackCandidate(source, snapshot, System.currentTimeMillis())
                else -> previous.copy(snapshot = snapshot)
            }
        }

    /** Re-evaluates the selection periodically so a long-paused track eventually disappears. */
    private fun clock(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis() / CLOCK_BUCKET_MS)
            delay(CLOCK_BUCKET_MS)
        }
    }

    private companion object {
        const val CLOCK_BUCKET_MS = 30_000L
    }
}
