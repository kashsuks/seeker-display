package com.kashsuks.seekerdisplay.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.transformLatest
import java.time.ZoneId
import java.time.ZonedDateTime

object TimeTicker {
    /** Emits the current time now, then at every [periodMs] boundary of the wall clock. */
    fun ticks(context: Context, periodMs: Long = 60_000L): Flow<ZonedDateTime> {
        val appContext = context.applicationContext
        return merge(flowOf(Unit), clockChanges(appContext)).transformLatest {
            while (true) {
                val now = ZonedDateTime.now()
                emit(now)
                delay(delayUntilNextBoundary(System.currentTimeMillis(), periodMs))
            }
        }.conflate()
    }

    /** Fires when the system time or timezone is changed so the next boundary is recomputed. */
    private fun clockChanges(context: Context): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    /** Milliseconds from [nowMs] until the next multiple of [periodMs] (always at least 1). */
    fun delayUntilNextBoundary(nowMs: Long, periodMs: Long): Long {
        val remainder = nowMs % periodMs
        return (periodMs - remainder).coerceAtLeast(1L)
    }
}
