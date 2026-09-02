package com.routecj.driver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.routecj.driver.MainActivity
import com.routecj.driver.core.util.ConnectivityMonitor
import kotlinx.coroutines.*
import java.util.Date

/**
 * Android Foreground Service providing real-time background GPS tracking for active Driver trips.
 * Synchronizes coordinates to Cloud Firestore under `drivers/{driverId}` and updates vehicle/dispatches if applicable.
 */
class DriverLocationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var currentDriverId: String = ""
    private var currentTripId: String = ""
    private var currentOrderNumber: String = ""

    private var isOnline: Boolean = true
    private var pendingLocation: Location? = null
    private lateinit var connectivityMonitor: ConnectivityMonitor
    private var networkMonitorJob: Job? = null

    private var lastUploadedLocation: Location? = null
    private var lastUploadTimeMs: Long = 0L
    private var hasReceivedFreshFix = false

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val locations = result.locations
                val location = result.lastLocation ?: locations.lastOrNull()
                if (location != null) {
                    handleNewLocation(location, isLastKnown = false)
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val isGpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                        lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

                if (!isGpsEnabled) {
                    DriverLocationStateHolder.updateState(DriverGpsState.LocationDisabled)
                } else if (!availability.isLocationAvailable && !hasReceivedFix()) {
                    DriverLocationStateHolder.updateState(DriverGpsState.WaitingForSignal)
                }
            }
        }

        connectivityMonitor = ConnectivityMonitor(this)
        networkMonitorJob = serviceScope.launch {
            connectivityMonitor.isConnected.collect { online ->
                isOnline = online
                val currentGps = DriverLocationStateHolder.gpsState.value
                if (currentGps is DriverGpsState.Active) {
                    DriverLocationStateHolder.updateState(
                        currentGps.copy(isOffline = !online)
                    )
                }
                if (online && pendingLocation != null) {
                    val loc = pendingLocation
                    pendingLocation = null
                    loc?.let { handleNewLocation(it) }
                }
            }
        }
    }

    private fun hasReceivedFix(): Boolean {
        return DriverLocationStateHolder.gpsState.value is DriverGpsState.Active
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_TRACKING) {
            stopTracking()
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_TRACKING) {
            val driverId = intent.getStringExtra(EXTRA_DRIVER_ID) ?: ""
            val tripId = intent.getStringExtra(EXTRA_TRIP_ID) ?: ""
            val orderNumber = intent.getStringExtra(EXTRA_ORDER_NUMBER) ?: "Active Trip"

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        buildOngoingNotification(orderNumber),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } else {
                    startForeground(NOTIFICATION_ID, buildOngoingNotification(orderNumber))
                }
            } catch (e: Exception) {
                android.util.Log.e("DriverLocationService", "Failed to startForeground immediately", e)
                DriverLocationStateHolder.updateState(DriverGpsState.StartFailed)
                stopSelf()
                return START_NOT_STICKY
            }

            if (driverId.isBlank() || tripId.isBlank()) {
                android.util.Log.e("DriverLocationService", "Invalid intent extras: driverId=$driverId, tripId=$tripId")
                stopSelf()
                return START_NOT_STICKY
            }

            val authUid = auth.currentUser?.uid
            if (authUid == null) {
                DriverLocationStateHolder.updateState(DriverGpsState.StartFailed)
                stopSelf()
                return START_NOT_STICKY
            }

            currentDriverId = if (driverId.isNotBlank()) driverId else authUid
            currentTripId = tripId
            currentOrderNumber = orderNumber

            startLocationUpdates()
        }

        return START_STICKY
    }

    private fun startLocationUpdates() {
        val fineLocationPermission = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED &&
            coarseLocationPermission != PackageManager.PERMISSION_GRANTED
        ) {
            DriverLocationStateHolder.updateState(DriverGpsState.PermissionRequired)
            return
        }

        val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val isNetworkEnabled = lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        // Safe Diagnostics
        android.util.Log.d(
            "DriverLocationService",
            "GPS Provider Enabled: $isGpsEnabled, Network Provider Enabled: $isNetworkEnabled"
        )

        if (!isGpsEnabled && !isNetworkEnabled) {
            DriverLocationStateHolder.updateState(DriverGpsState.LocationDisabled)
            return
        }

        if (DriverLocationStateHolder.gpsState.value !is DriverGpsState.Active) {
            DriverLocationStateHolder.updateState(DriverGpsState.WaitingForSignal)
        }

        // Fast Location Acquisition: Check Fused + System cached locations immediately
        tryAcquireCachedLocation()

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LOCATION_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            setMinUpdateDistanceMeters(0f) // Allow updates even when stationary
            setWaitForAccurateLocation(false)
        }.build()

        try {
            fusedLocationClient.removeLocationUpdates(locationCallback) // Ensure single callback
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            ).addOnFailureListener { e ->
                android.util.Log.e("DriverLocationService", "FusedLocationClient request failed", e)
                DriverLocationStateHolder.updateState(DriverGpsState.StartFailed)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("DriverLocationService", "Location permission security exception", e)
            DriverLocationStateHolder.updateState(DriverGpsState.PermissionRequired)
        } catch (e: Exception) {
            android.util.Log.e("DriverLocationService", "Unexpected error starting location updates", e)
            DriverLocationStateHolder.updateState(DriverGpsState.StartFailed)
        }
    }

    private fun tryAcquireCachedLocation() {
        val fineLocationPermission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocationPermission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED && coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null && isLocationValid(lastLoc) && !hasReceivedFreshFix) {
                    handleNewLocation(lastLoc, isLastKnown = true)
                } else if (!hasReceivedFreshFix) {
                    trySystemLastKnownLocation()
                }
            }.addOnFailureListener {
                if (!hasReceivedFreshFix) {
                    trySystemLastKnownLocation()
                }
            }
        } catch (_: Exception) {
            trySystemLastKnownLocation()
        }
    }

    private fun trySystemLastKnownLocation() {
        if (hasReceivedFreshFix) return
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
            val gpsLoc = if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null
            val netLoc = if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null

            val bestLoc = listOfNotNull(gpsLoc, netLoc).maxByOrNull { it.time }
            if (bestLoc != null && isLocationValid(bestLoc)) {
                handleNewLocation(bestLoc, isLastKnown = true)
            }
        } catch (_: Exception) {}
    }

    private fun isLocationValid(location: Location): Boolean {
        return !location.latitude.isNaN() && !location.longitude.isNaN() &&
                !location.latitude.isInfinite() && !location.longitude.isInfinite() &&
                !(location.latitude == 0.0 && location.longitude == 0.0)
    }

    private fun handleNewLocation(location: Location, isLastKnown: Boolean = false) {
        if (!isLastKnown) {
            hasReceivedFreshFix = true
        }

        if (!isLocationValid(location)) {
            return
        }

        val now = Date()

        // Publish live GPS state to Compose UI and map overlays
        DriverLocationStateHolder.updateState(
            DriverGpsState.Active(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                speed = location.speed,
                bearing = if (location.hasBearing()) location.bearing else 0f,
                timestamp = now,
                tripId = currentTripId,
                isOffline = !isOnline,
                isLastKnownLocation = isLastKnown
            )
        )

        if (isLastKnown) {
            return
        }

        if (location.accuracy > 100f) {
            return
        }

        if (!isOnline) {
            pendingLocation = location
            return
        }

        val timeSinceLastUpload = System.currentTimeMillis() - lastUploadTimeMs
        val distanceSinceLastUpload = lastUploadedLocation?.distanceTo(location) ?: Float.MAX_VALUE

        if (distanceSinceLastUpload < MIN_UPLOAD_DISTANCE_METERS_UPLOAD && timeSinceLastUpload < MAX_UPLOAD_INTERVAL_MS) {
            return
        }

        lastUploadedLocation = location
        lastUploadTimeMs = System.currentTimeMillis()

        serviceScope.launch {
            try {
                if (currentDriverId.isNotBlank()) {
                    val driverLocationUpdates = mapOf(
                        "currentLatitude" to location.latitude,
                        "currentLongitude" to location.longitude,
                        "accuracy" to location.accuracy,
                        "speed" to location.speed,
                        "lastActive" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "activeTripId" to currentTripId,
                        "isLocationSharing" to true
                    )

                    firestore.collection("drivers")
                        .document(currentDriverId)
                        .set(driverLocationUpdates, SetOptions.merge())

                    val liveLocationDoc = mapOf(
                        "driverId" to currentDriverId,
                        "tripId" to currentTripId,
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "accuracy" to location.accuracy,
                        "speed" to location.speed,
                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "isLocationSharing" to true
                    )
                    firestore.collection("driverLocations")
                        .document(currentDriverId)
                        .set(liveLocationDoc, SetOptions.merge())
                }
            } catch (_: Exception) {}
        }
    }

    private fun stopTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}

        if (currentDriverId.isNotBlank()) {
            serviceScope.launch {
                try {
                    firestore.collection("drivers")
                        .document(currentDriverId)
                        .update(
                            mapOf(
                                "isLocationSharing" to false,
                                "lastActive" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )
                        )
                    firestore.collection("driverLocations")
                        .document(currentDriverId)
                        .update(
                            mapOf(
                                "isLocationSharing" to false,
                                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )
                        )
                } catch (_: Exception) {}
            }
        }

        hasReceivedFreshFix = false
        DriverLocationStateHolder.updateState(DriverGpsState.Inactive)
    }

    override fun onDestroy() {
        stopTracking()
        networkMonitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildOngoingNotification(orderNumber: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RouteCJ Driver • Live GPS Active")
            .setContentText("Live delivery tracking for Trip: $orderNumber")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "RouteCJ Driver Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing notification while live GPS tracking is active for deliveries."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "routecj_driver_location_channel"
        const val NOTIFICATION_ID = 4040

        const val ACTION_START_TRACKING = "com.routecj.driver.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.routecj.driver.ACTION_STOP_TRACKING"

        const val EXTRA_DRIVER_ID = "extra_driver_id"
        const val EXTRA_TRIP_ID = "extra_trip_id"
        const val EXTRA_ORDER_NUMBER = "extra_order_number"

        const val LOCATION_INTERVAL_MS = 10000L
        const val FASTEST_INTERVAL_MS = 5000L

        const val MIN_UPLOAD_DISTANCE_METERS_UPLOAD = 5.0f
        const val MAX_UPLOAD_INTERVAL_MS = 30000L

        fun start(context: Context, driverId: String, tripId: String, orderNumber: String) {
            try {
                if (DriverLocationStateHolder.gpsState.value == DriverGpsState.Inactive) {
                    DriverLocationStateHolder.updateState(DriverGpsState.Connecting)
                }

                val intent = Intent(context, DriverLocationService::class.java).apply {
                    action = ACTION_START_TRACKING
                    putExtra(EXTRA_DRIVER_ID, driverId)
                    putExtra(EXTRA_TRIP_ID, tripId)
                    putExtra(EXTRA_ORDER_NUMBER, orderNumber)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("DriverLocationService", "Failed to start foreground service", e)
                DriverLocationStateHolder.updateState(DriverGpsState.StartFailed)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, DriverLocationService::class.java).apply {
                action = ACTION_STOP_TRACKING
            }
            context.startService(intent)
        }
    }
}
