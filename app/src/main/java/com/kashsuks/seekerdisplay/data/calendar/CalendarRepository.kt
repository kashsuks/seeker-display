package com.kashsuks.seekerdisplay.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.kashsuks.seekerdisplay.core.TimeTicker
import com.kashsuks.seekerdisplay.data.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.ZonedDateTime

/** A calendar the device syncs, listed in Settings so individual ones can be switched off. */
data class DeviceCalendar(
    val id: Long,
    val name: String,
    val account: String,
    val color: Int,
)

/**
 * Reads events from Android's calendar provider. Any Google (or Exchange) account added in system
 * settings is synced there by the OS, so this one source covers every account.
 */
class CalendarRepository(
    private val context: Context,
    private val settings: SettingsRepository,
) {
    private val resolver = context.contentResolver
    private val permissionChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Re-query right away, e.g. just after READ_CALENDAR is granted. */
    fun refreshNow() {
        permissionChanges.tryEmit(Unit)
    }

    /** Upcoming events, refreshed when the provider changes, every minute, and when settings change. */
    val upcoming: Flow<List<CalendarEvent>> = combine(
        merge(providerChanges(), TimeTicker.ticks(context).map { }, permissionChanges).onStart { emit(Unit) },
        settings.hiddenCalendarIds,
    ) { _, hidden -> hidden }
        .conflate()
        .map { hidden -> loadUpcoming(hidden) }
        .flowOn(Dispatchers.IO)

    private fun providerChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    private fun loadUpcoming(hidden: Set<Long>): List<CalendarEvent> {
        if (!hasPermission()) return emptyList()
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val windowStart = now.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val windowEnd = now.plusDays(WINDOW_DAYS).toInstant().toEpochMilli()
        return try {
            UpcomingEvents.build(queryInstances(windowStart, windowEnd), now.toInstant().toEpochMilli(), zone, hidden)
        } catch (e: CancellationException) {
            throw e
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    private fun queryInstances(beginMs: Long, endMs: Long): List<RawInstance> {
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, beginMs)
            ContentUris.appendId(it, endMs)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_COLOR,
            CalendarContract.Instances.CALENDAR_COLOR,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS,
        )
        val out = ArrayList<RawInstance>()
        resolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext()) {
                out += RawInstance(
                    eventId = c.getLong(0),
                    title = c.getString(1),
                    beginMs = c.getLong(2),
                    endMs = c.getLong(3),
                    allDay = c.getInt(4) == 1,
                    eventColor = c.getInt(5),
                    calendarColor = c.getInt(6),
                    calendarId = c.getLong(7),
                    selfAttendeeStatus = c.getInt(8),
                )
            }
        }
        return out
    }

    /** Every calendar synced to the device, for the Settings toggles. */
    suspend fun calendars(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
        )
        val out = ArrayList<DeviceCalendar>()
        resolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, "${CalendarContract.Calendars.ACCOUNT_NAME} ASC")?.use { c ->
            while (c.moveToNext()) {
                out += DeviceCalendar(c.getLong(0), c.getString(1).orEmpty(), c.getString(2).orEmpty(), c.getInt(3) or 0xFF000000.toInt())
            }
        }
        out
    }

    private companion object {
        const val WINDOW_DAYS = 7L
    }
}
