package net.wshmkr.launcher.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.datastore.WeatherCacheDataSource
import net.wshmkr.launcher.model.WeatherReading
import net.wshmkr.launcher.model.WeatherUiState
import net.wshmkr.launcher.util.ONE_HOUR
import net.wshmkr.launcher.util.ONE_MINUTE
import net.wshmkr.launcher.util.WeatherHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userSettingsDataSource: UserSettingsDataSource,
    private val weatherCacheDataSource: WeatherCacheDataSource,
) {
    companion object {
        private const val READING_TTL_MS = 30L * ONE_MINUTE
        private const val MAX_READING_AGE_MS = 6L * ONE_HOUR
        private const val CACHE_LOCATION_RADIUS_METERS = 5_000f

        private const val MIN_LOOP_DELAY_MS = 1L * ONE_MINUTE
    }

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val fetchMutex = Mutex()

    private val _state = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private val forceRefreshRequests = Channel<Unit>(capacity = Channel.CONFLATED)

    private var reading: WeatherReading? = null
    private var nextRefreshDueAtMillis = 0L

    private val initialLoad: Job by lazy {
        scope.launch {
            val persisted = weatherCacheDataSource.load() ?: return@launch
            reading = persisted
            val now = System.currentTimeMillis()
            // Resolving to Error keeps the spinner reachable only on a true first run.
            _state.value = if (persisted.isDisplayableAt(now)) {
                WeatherUiState.Ready(
                    persisted,
                    isStale = now - persisted.fetchedAtMillis >= READING_TTL_MS
                )
            } else {
                WeatherUiState.Error
            }
        }
    }

    init {
        scope.launch {
            _state.subscriptionCount
                .map { it > 0 }
                .distinctUntilChanged()
                .collectLatest { subscribed ->
                    if (!subscribed) return@collectLatest
                    var force = false
                    while (true) {
                        refresh(force)
                        force = withTimeoutOrNull(delayUntilNextRefresh()) {
                            forceRefreshRequests.receive()
                        } != null
                    }
                }
        }

        scope.launch {
            userSettingsDataSource.weatherLocation
                .drop(1)
                .collect { requestRefresh() }
        }
    }

    fun requestRefresh() {
        forceRefreshRequests.trySend(Unit)
    }

    private suspend fun refresh(force: Boolean) {
        initialLoad.join()
        fetchMutex.withLock {
            val now = System.currentTimeMillis()
            nextRefreshDueAtMillis = now + READING_TTL_MS
            val staticLocation = userSettingsDataSource.weatherLocation.first()
            if (staticLocation == null && !WeatherHelper.isLocationGranted(context)) return

            val current = reading
            val servable = current
                ?.takeIf { now - it.fetchedAtMillis < READING_TTL_MS }
                ?.takeIf { staticLocation == null || it.isNear(staticLocation.first, staticLocation.second) }
            if (!force && servable != null) {
                _state.value = WeatherUiState.Ready(servable, isStale = false)
                nextRefreshDueAtMillis = servable.fetchedAtMillis + READING_TTL_MS
                return
            }

            val target = staticLocation ?: deviceLocation()
            if (target == null) {
                fallBackToCache(current, now)
                return
            }

            WeatherHelper.fetchCurrentWeather(target.first, target.second)
                .onSuccess { fetched ->
                    reading = fetched
                    weatherCacheDataSource.save(fetched)
                    _state.value = WeatherUiState.Ready(fetched, isStale = false)
                    nextRefreshDueAtMillis = fetched.fetchedAtMillis + READING_TTL_MS
                }
                .onFailure {
                    fallBackToCache(current, now)
                }
        }
    }

    private fun fallBackToCache(current: WeatherReading?, now: Long) {
        val usable = current?.takeIf { it.isDisplayableAt(now) }
        _state.value = usable?.let { WeatherUiState.Ready(it, isStale = true) }
            ?: WeatherUiState.Error
    }

    private fun delayUntilNextRefresh(): Long =
        (nextRefreshDueAtMillis - System.currentTimeMillis()).coerceAtLeast(MIN_LOOP_DELAY_MS)

    // Permission is checked dynamically in refresh() before this is reached.
    @SuppressLint("MissingPermission")
    private suspend fun deviceLocation(): Pair<Double, Double>? =
        runCatching { WeatherHelper.getBestAvailableLocation(fusedClient) }.getOrNull()
            ?.let { it.latitude to it.longitude }

    private fun WeatherReading.isDisplayableAt(now: Long): Boolean =
        now - fetchedAtMillis < MAX_READING_AGE_MS

    private fun WeatherReading.isNear(targetLatitude: Double, targetLongitude: Double): Boolean {
        val distance = FloatArray(1)
        Location.distanceBetween(latitude, longitude, targetLatitude, targetLongitude, distance)
        return distance[0] <= CACHE_LOCATION_RADIUS_METERS
    }
}
