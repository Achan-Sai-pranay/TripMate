package com.example.tripmate.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class TrackedStop(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val arrivalTime: String
)

object TripLocationTracker : LocationListener {

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _trackedStops = MutableStateFlow<List<TrackedStop>>(emptyList())
    val trackedStops: StateFlow<List<TrackedStop>> = _trackedStops.asStateFlow()

    private val _totalDistanceMeters = MutableStateFlow(0f)
    val totalDistanceMeters: StateFlow<Float> = _totalDistanceMeters.asStateFlow()

    private var locationManager: LocationManager? = null
    private var lastRecordedLocation: Location? = null
    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    @SuppressLint("MissingPermission")
    fun startTracking(context: Context) {
        appContext = context.applicationContext
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        locationManager = lm

        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    4000L,
                    10f,
                    this
                )
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    4000L,
                    10f,
                    this
                )
            }

            val lastGps = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
            val lastNet = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }
            val bestLast = lastGps ?: lastNet
            if (bestLast != null) {
                onLocationChanged(bestLast)
            }

            _isTracking.value = true
        } catch (_: SecurityException) {
            _isTracking.value = false
        }
    }

    fun stopTracking() {
        locationManager?.let { lm ->
            try {
                lm.removeUpdates(this)
            } catch (_: Exception) {}
        }
        locationManager = null
        _isTracking.value = false
    }

    fun toggleTracking(context: Context) {
        if (_isTracking.value) {
            stopTracking()
        } else {
            startTracking(context)
        }
    }

    fun clearStops() {
        _trackedStops.value = emptyList()
        _totalDistanceMeters.value = 0f
        lastRecordedLocation = null
    }

    override fun onLocationChanged(location: Location) {
        val previous = lastRecordedLocation
        if (previous != null) {
            val dist = previous.distanceTo(location)
            if (dist > 5f) {
                _totalDistanceMeters.value += dist
            }

            // Record a stop when user travels more than 150 meters
            if (dist > 150f) {
                recordStop(location)
                lastRecordedLocation = location
            }
        } else {
            lastRecordedLocation = location
            recordStop(location)
        }
        _currentLocation.value = location
    }

    private fun recordStop(location: Location) {
        val now = System.currentTimeMillis()
        val timeLabel = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date(now))
        val stopIndex = _trackedStops.value.size + 1

        val initialStop = TrackedStop(
            id = "stop_${System.currentTimeMillis()}",
            name = "Visited Stop #$stopIndex",
            address = String.format(Locale.US, "%.4f, %.4f", location.latitude, location.longitude),
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = now,
            arrivalTime = timeLabel
        )

        _trackedStops.value = _trackedStops.value + initialStop

        // Reverse-geocode asynchronously
        val ctx = appContext
        if (ctx != null && Geocoder.isPresent()) {
            scope.launch {
                try {
                    val geocoder = Geocoder(ctx, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                            val addr = addresses.firstOrNull() ?: return@getFromLocation
                            val placeName = addr.featureName ?: addr.locality ?: "Visited Stop #$stopIndex"
                            val fullAddress = addr.getAddressLine(0) ?: ""
                            updateStopDetails(initialStop.id, placeName, fullAddress)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val addr = addresses?.firstOrNull()
                        if (addr != null) {
                            val placeName = addr.featureName ?: addr.locality ?: "Visited Stop #$stopIndex"
                            val fullAddress = addr.getAddressLine(0) ?: ""
                            updateStopDetails(initialStop.id, placeName, fullAddress)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun updateStopDetails(id: String, name: String, address: String) {
        _trackedStops.value = _trackedStops.value.map {
            if (it.id == id) it.copy(name = name, address = address) else it
        }
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
