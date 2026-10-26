package com.kashsuks.seekerdisplay.data.media

/** A source's current state plus when it last started/stopped or changed track. */
data class PlaybackCandidate(
    val source: MediaSource,
    val snapshot: PlaybackSnapshot,
    val lastChangeMs: Long,
)

/** Decides which source the dashboard shows when more than one has something. */
object PlaybackSelector {
    /** How long a paused track stays on the dashboard before it is hidden. */
    const val PAUSED_VISIBLE_MS = 10 * 60 * 1000L

    fun select(candidates: List<PlaybackCandidate>, nowMs: Long): PlaybackCandidate? {
        val playing = candidates.filter { it.snapshot.isPlaying }
        if (playing.isNotEmpty()) return playing.maxBy { it.lastChangeMs }
        return candidates
            .filter { nowMs - it.lastChangeMs <= PAUSED_VISIBLE_MS }
            .maxByOrNull { it.lastChangeMs }
    }
}
