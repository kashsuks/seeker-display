package com.kashsuks.seekerdisplay.data.bridge

import android.os.SystemClock
import com.kashsuks.seekerdisplay.data.media.MediaSource
import com.kashsuks.seekerdisplay.data.media.MediaSourceType
import com.kashsuks.seekerdisplay.data.media.PlaybackSnapshot
import com.kashsuks.seekerdisplay.data.settings.SettingsRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Spotify playing on the user's Mac, via the companion bridge script on the local network. */
class MacBridgeSource(
    private val client: HttpClient,
    private val settings: SettingsRepository,
    private val discovery: BridgeDiscovery,
) : MediaSource {
    override val type = MediaSourceType.MAC

    @Volatile private var fast = false
    @Volatile private var target: Target? = null
    @Volatile private var last: PlaybackSnapshot? = null

    private data class Target(val endpoint: BridgeEndpoint, val token: String)

    /** Wakes the poll loop early, e.g. right after a control press, to reconcile with reality. */
    private val wake = Channel<Unit>(Channel.CONFLATED)

    /** Immediate guesses shown after a control press, before the next poll confirms them. */
    private val optimistic = MutableSharedFlow<PlaybackSnapshot?>(extraBufferCapacity = 4)

    private val endpoints: Flow<BridgeEndpoint?> = settings.bridgeAddress.flatMapLatest { manual ->
        if (manual.isNotBlank()) flowOf(BridgeEndpoint.parse(manual)) else discovery.endpoint.onStart { emit(null) }
    }

    override val state: Flow<PlaybackSnapshot?> = combine(endpoints, settings.bridgeToken) { endpoint, token ->
        if (endpoint != null && token.isNotBlank()) Target(endpoint, token) else null
    }.flatMapLatest { current ->
        target = current
        if (current == null) flowOf(null) else merge(poll(current), optimistic)
    }

    override fun setFastPolling(enabled: Boolean) {
        fast = enabled
        wake.trySend(Unit)
    }

    private fun poll(current: Target): Flow<PlaybackSnapshot?> = flow {
        var failures = 0
        while (true) {
            try {
                val snapshot = fetchState(current)
                failures = 0
                last = snapshot
                emit(snapshot)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failures++
                // Tolerate a couple of dropped polls before hiding the card.
                if (failures >= FAILURES_BEFORE_HIDING) {
                    last = null
                    emit(null)
                }
            }
            val wait = when {
                failures > 0 -> minOf(MAX_BACKOFF_MS, BASE_BACKOFF_MS shl minOf(failures, 3))
                fast -> FAST_POLL_MS
                else -> SLOW_POLL_MS
            }
            withTimeoutOrNull(wait) { wake.receive() }
        }
    }

    private suspend fun fetchState(target: Target): PlaybackSnapshot? {
        val response: HttpResponse = client.get("${target.endpoint.baseUrl}/state") {
            header(HttpHeaders.Authorization, "Bearer ${target.token}")
            timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
        }
        if (response.status != HttpStatusCode.OK) throw IllegalStateException("bridge returned ${response.status}")
        return response.body<BridgeStateDto>().toSnapshot(SystemClock.elapsedRealtime())
    }

    private suspend fun post(path: String, body: kotlinx.serialization.json.JsonObject? = null) {
        val current = target ?: return
        client.post("${current.endpoint.baseUrl}$path") {
            header(HttpHeaders.Authorization, "Bearer ${current.token}")
            timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }
        wake.trySend(Unit)
    }

    private fun guess(change: (PlaybackSnapshot, Long) -> PlaybackSnapshot) {
        val now = SystemClock.elapsedRealtime()
        last?.let { snapshot ->
            val updated = change(snapshot, now)
            last = updated
            optimistic.tryEmit(updated)
        }
    }

    override suspend fun playPause() {
        guess { s, now -> s.copy(isPlaying = !s.isPlaying, positionMs = s.positionAt(now), capturedAtElapsedRealtime = now) }
        post("/playpause")
    }

    override suspend fun next() = post("/next")

    override suspend fun previous() = post("/previous")

    override suspend fun seekTo(positionMs: Long) {
        guess { s, now -> s.copy(positionMs = positionMs, capturedAtElapsedRealtime = now) }
        post("/seek", buildJsonObject { put("positionMs", positionMs) })
    }

    override suspend fun setShuffle(enabled: Boolean) {
        guess { s, _ -> s.copy(shuffle = enabled) }
        post("/shuffle", buildJsonObject { put("on", enabled) })
    }

    override suspend fun setRepeat(enabled: Boolean) {
        guess { s, _ -> s.copy(repeat = enabled) }
        post("/repeat", buildJsonObject { put("on", enabled) })
    }

    /** One-off connection check for the Settings screen; returns a message for the user. */
    suspend fun testConnection(): String {
        val token = settings.bridgeToken.first()
        if (token.isBlank()) return "Enter the access token first."
        val manual = settings.bridgeAddress.first()
        val endpoint = if (manual.isNotBlank()) {
            BridgeEndpoint.parse(manual) ?: return "That address doesn't look right."
        } else {
            withTimeoutOrNull(DISCOVERY_TIMEOUT_MS) { discovery.endpoint.first { it != null } }
                ?: return "Couldn't find the bridge on this network. Is it running, and are both on the same Wi-Fi?"
        }
        return try {
            val response: HttpResponse = client.get("${endpoint!!.baseUrl}/state") {
                header(HttpHeaders.Authorization, "Bearer $token")
                timeout { requestTimeoutMillis = REQUEST_TIMEOUT_MS }
            }
            when (response.status) {
                HttpStatusCode.OK -> {
                    val dto = response.body<BridgeStateDto>()
                    val where = dto.host.ifBlank { endpoint.host }
                    if (dto.running) "Connected to $where." else "Connected to $where, but Spotify isn't running there."
                }
                HttpStatusCode.Unauthorized -> "Reached the bridge, but the token was rejected."
                else -> "Bridge answered with ${response.status.value}."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            "Couldn't reach ${endpoint!!.host}:${endpoint.port}."
        }
    }

    private companion object {
        const val FAST_POLL_MS = 1_000L
        const val SLOW_POLL_MS = 3_000L
        const val BASE_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 15_000L
        const val REQUEST_TIMEOUT_MS = 4_000L
        const val DISCOVERY_TIMEOUT_MS = 6_000L
        const val FAILURES_BEFORE_HIDING = 3
    }
}
