package com.kashsuks.seekerdisplay.data.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart

/** Spotify playing on this phone, read from its media session. Needs notification access. */
class LocalMediaSessionSource(private val context: Context) : MediaSource {
    override val type = MediaSourceType.PHONE

    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, MediaListenerService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val restarts = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    @Volatile
    private var controller: MediaController? = null

    fun isAccessGranted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /** Re-attach to sessions, e.g. right after the user grants notification access. */
    fun restart() {
        restarts.tryEmit(Unit)
    }

    override val state: Flow<PlaybackSnapshot?> = restarts.onStart { emit(Unit) }.flatMapLatest { sessions() }

    private fun sessions(): Flow<PlaybackSnapshot?> = callbackFlow {
        if (!isAccessGranted()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val tracker = SessionTracker(this)
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { tracker.select(it) }
        try {
            manager.addOnActiveSessionsChangedListener(listener, listenerComponent, handler)
            tracker.select(manager.getActiveSessions(listenerComponent))
        } catch (_: SecurityException) {
            trySend(null)
        }
        awaitClose {
            runCatching { manager.removeOnActiveSessionsChangedListener(listener) }
            tracker.release()
            controller = null
        }
    }

    /** Follows the Spotify session and publishes a snapshot whenever its metadata or state changes. */
    private inner class SessionTracker(private val out: ProducerScope<PlaybackSnapshot?>) : MediaController.Callback() {
        private var active: MediaController? = null

        fun select(controllers: List<MediaController>?) {
            val spotify = controllers?.firstOrNull { it.packageName == SPOTIFY_PACKAGE }
            if (spotify?.sessionToken != active?.sessionToken) {
                active?.unregisterCallback(this)
                active = spotify
                spotify?.registerCallback(this, handler)
            }
            controller = active
            publish()
        }

        fun release() {
            active?.unregisterCallback(this)
            active = null
        }

        private fun publish() {
            out.trySend(active?.let(::toSnapshot))
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = select(manager.getActiveSessions(listenerComponent))
    }

    private fun toSnapshot(controller: MediaController): PlaybackSnapshot? {
        val metadata = controller.metadata ?: return null
        val playback = controller.playbackState ?: return null
        if (playback.state == PlaybackState.STATE_NONE || playback.state == PlaybackState.STATE_STOPPED) return null
        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: return null

        val playing = playback.state == PlaybackState.STATE_PLAYING || playback.state == PlaybackState.STATE_BUFFERING
        val artwork: Bitmap? = listOf(MediaMetadata.METADATA_KEY_ALBUM_ART, MediaMetadata.METADATA_KEY_ART, MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            .firstNotNullOfOrNull { metadata.getBitmap(it) }
        val artworkUrl = listOf(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, MediaMetadata.METADATA_KEY_ART_URI, MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)
            .firstNotNullOfOrNull { metadata.getString(it)?.takeIf { uri -> uri.startsWith("http") } }

        return PlaybackSnapshot(
            source = MediaSourceType.PHONE,
            sourceLabel = "On this phone",
            isPlaying = playing,
            title = title,
            artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            artwork = artwork,
            artworkUrl = artworkUrl,
            durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0),
            positionMs = playback.position.coerceAtLeast(0),
            capturedAtElapsedRealtime = playback.lastPositionUpdateTime,
        )
    }

    override suspend fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play()
    }

    override suspend fun next() {
        controller?.transportControls?.skipToNext()
    }

    override suspend fun previous() {
        controller?.transportControls?.skipToPrevious()
    }

    override suspend fun seekTo(positionMs: Long) {
        controller?.transportControls?.seekTo(positionMs)
    }

    private companion object {
        const val SPOTIFY_PACKAGE = "com.spotify.music"
    }
}
