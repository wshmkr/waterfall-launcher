package net.wshmkr.launcher.repository

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.wshmkr.launcher.model.CalendarEvent
import net.wshmkr.launcher.util.ONE_SECOND
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val refreshTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun requestRefresh() {
        refreshTrigger.tryEmit(Unit)
    }

    fun observeTodayEvents(maxEvents: Int = DEFAULT_MAX_EVENTS): Flow<List<CalendarEvent>> = callbackFlow {
        val invalidations = Channel<Unit>(capacity = Channel.CONFLATED)

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                invalidations.trySend(Unit)
            }
        }
        context.contentResolver.registerContentObserver(
            CalendarContract.Events.CONTENT_URI,
            true,
            observer,
        )

        val refreshJob = launch {
            refreshTrigger.collect { invalidations.trySend(Unit) }
        }

        val worker = launch {
            var events = queryTodayEvents(maxEvents)
            trySend(events)
            while (isActive) {
                withTimeoutOrNull(nextInvalidationDelay(events)) { invalidations.receive() }
                events = queryTodayEvents(maxEvents)
                trySend(events)
            }
        }

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
            worker.cancel()
            refreshJob.cancel()
        }
    }

    private suspend fun queryTodayEvents(maxEvents: Int): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            if (!hasReadCalendarPermission(context)) return@withContext emptyList()

            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
            val startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val now = System.currentTimeMillis()
            val begin = maxOf(now, startOfDay)

            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .also {
                    ContentUris.appendId(it, begin)
                    ContentUris.appendId(it, startOfTomorrow)
                }
                .build()

            val projection = arrayOf(
                CalendarContract.Instances._ID,
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_COLOR,
            )

            val selection = "${CalendarContract.Instances.VISIBLE} = 1 AND " +
                "${CalendarContract.Instances.END} >= ?"
            val selectionArgs = arrayOf(now.toString())
            val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

            val events = mutableListOf<CalendarEvent>()
            try {
                context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                    ?.use { cursor ->
                        val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances._ID)
                        val eventIdIdx =
                            cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                        val titleIdx =
                            cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                        val beginIdx =
                            cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                        val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                        val allDayIdx =
                            cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                        val colorIdx =
                            cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_COLOR)

                        while (cursor.moveToNext() && events.size < maxEvents) {
                            val title = cursor.getString(titleIdx)?.takeIf { it.isNotBlank() }
                                ?: continue
                            val rawBegin = cursor.getLong(beginIdx)
                            events.add(
                                CalendarEvent(
                                    instanceId = cursor.getLong(idIdx),
                                    eventId = cursor.getLong(eventIdIdx),
                                    title = title,
                                    startMillis = maxOf(rawBegin, startOfDay),
                                    endMillis = cursor.getLong(endIdx),
                                    allDay = cursor.getInt(allDayIdx) != 0,
                                    calendarColor = cursor.getInt(colorIdx).takeIf { it != 0 },
                                )
                            )
                        }
                    }
            } catch (e: SecurityException) {
                Log.w(TAG, "READ_CALENDAR revoked mid-query", e)
                return@withContext emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query calendar instances", e)
                return@withContext emptyList()
            }
            events
        }

    private fun nextInvalidationDelay(events: List<CalendarEvent>): Long {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val midnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val earliestEnd = events.mapNotNull { event ->
            event.endMillis.takeIf { it > now }
        }.minOrNull()
        val nextBoundary = earliestEnd?.let { minOf(it, midnight) } ?: midnight
        return (nextBoundary - now + ONE_SECOND).coerceAtLeast(ONE_SECOND.toLong())
    }

    companion object {
        const val DEFAULT_MAX_EVENTS = 3
        private const val TAG = "CalendarRepository"

        fun hasReadCalendarPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED
    }
}
