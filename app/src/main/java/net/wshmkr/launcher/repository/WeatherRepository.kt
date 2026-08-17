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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import net.wshmkr.launcher.datastore.UserSettingsDataSource
import net.wshmkr.launcher.datastore.WeatherCacheDataSource
import net.wshmkr.launcher.model.WeatherReading
import net.wshmkr.launcher.model.WeatherUiState
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
        // Fetch cadence: a reading younger than this is served without any location or network request.
        const val READING_TTL_MS = 30 * 60 * 1000L

        // Display ceiling: past this age a reading is no longer shown, even flagged stale.
        const val MAX_READING_AGE_MS = 6 * 60 * 60 * 1000L

        // Weather is the same across GPS jitter; a reading this close counts as the same place.
        private const val CACHE_LOCATION_RADIUS_METERS = 5_000f

        private const val MIN_LOOP_DELAY_MS = 60 * 1000L
    }

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val fetchMutex = Mutex()

    private val _state = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private val launcherVisible = MutableStateFlow(false)

    // Conflated so a request made mid-fetch or while hidden is kept until the loop can serve it.
    private val forceRefreshRequests = Channel<Unit>(capacity = Channel.CONFLATED)

    private var reading: WeatherReading? = null
    private var nextRefreshDueAtMillis = 0L

    private val initialLoad: Job = scope.launch {
        val persisted = weatherCacheDataSource.load() ?: return@launch
        fetchMutex.withLock {
            if (reading != null) return@launch
            reading = persisted
            val age = System.currentTimeMillis() - persisted.fetchedAtMillis
            if (age < MAX_READING_AGE_MS) {
                _state.value = WeatherUiState.Ready(persisted, isStale = age >= READING_TTL_MS)
            }
        }
    }

    init {
        scope.launch {
            combine(launcherVisible, userSettingsDataSource.showWeather) { visible, enabled ->
                visible && enabled
            }
                .distinctUntilChanged()
                .collectLatest { active ->
                    if (!active) return@collectLatest
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
            combine(userSettingsDataSource.weatherLat, userSettingsDataSource.weatherLon) { lat, lon ->
                lat to lon
            }
                .distinctUntilChanged()
                .drop(1)
                .collect { requestRefresh() }
        }
    }

    fun onLauncherVisible() {
        launcherVisible.value = true
    }

    fun onLauncherHidden() {
        launcherVisible.value = false
    }

    fun requestRefresh() {
        forceRefreshRequests.trySend(Unit)
    }

    private suspend fun refresh(force: Boolean) {
        initialLoad.join()
        fetchMutex.withLock {
            val now = System.currentTimeMillis()
            val staticLocation = staticLocation()
            if (staticLocation == null && !WeatherHelper.isLocationGranted(context)) {
                nextRefreshDueAtMillis = now + READING_TTL_MS
                return
            }

            val current = reading
            val fresh = current?.takeIf { now - it.fetchedAtMillis < READING_TTL_MS }
            val matchesTarget = fresh != null &&
                (staticLocation == null || fresh.isNear(staticLocation.first, staticLocation.second))
            if (!force && matchesTarget) {
                _state.value = WeatherUiState.Ready(fresh!!, isStale = false)
                nextRefreshDueAtMillis = fresh.fetchedAtMillis + READING_TTL_MS
                return
            }

            val target = staticLocation ?: deviceLocation()
            if (target == null) {
                fallBackToCache(current, now, "No location")
                return
            }

            WeatherHelper.fetchCurrentWeather(target.first, target.second)
                .onSuccess { fetched ->
                    reading = fetched
                    weatherCacheDataSource.save(fetched)
                    _state.value = WeatherUiState.Ready(fetched, isStale = false)
                    nextRefreshDueAtMillis = fetched.fetchedAtMillis + READING_TTL_MS
                }
                .onFailure { error ->
                    fallBackToCache(current, now, error.message ?: "Unable to load weather")
                }
        }
    }

    private fun fallBackToCache(current: WeatherReading?, now: Long, reason: String) {
        val usable = current?.takeIf { now - it.fetchedAtMillis < MAX_READING_AGE_MS }
        _state.value = usable?.let { WeatherUiState.Ready(it, isStale = true) }
            ?: WeatherUiState.Error(reason)
        nextRefreshDueAtMillis = now + READING_TTL_MS
    }

    private fun delayUntilNextRefresh(): Long =
        (nextRefreshDueAtMillis - System.currentTimeMillis()).coerceAtLeast(MIN_LOOP_DELAY_MS)

    private suspend fun staticLocation(): Pair<Double, Double>? {
        val latitude = userSettingsDataSource.weatherLat.first() ?: return null
        val longitude = userSettingsDataSource.weatherLon.first() ?: return null
        return latitude to longitude
    }

    // Permission is checked dynamically in refresh() before this is reached.
    @SuppressLint("MissingPermission")
    private suspend fun deviceLocation(): Pair<Double, Double>? =
        runCatching { WeatherHelper.getBestAvailableLocation(fusedClient) }.getOrNull()
            ?.let { it.latitude to it.longitude }

    private fun WeatherReading.isNear(targetLatitude: Double, targetLongitude: Double): Boolean {
        val distance = FloatArray(1)
        Location.distanceBetween(latitude, longitude, targetLatitude, targetLongitude, distance)
        return distance[0] <= CACHE_LOCATION_RADIUS_METERS
    }
}
