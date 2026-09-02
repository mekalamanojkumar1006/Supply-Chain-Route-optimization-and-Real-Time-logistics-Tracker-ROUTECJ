package com.routecj.customer.presentation.booking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.routecj.customer.domain.model.GeocodingResult
import com.routecj.customer.domain.model.Order
import com.routecj.customer.domain.model.OrderStatus
import com.routecj.customer.domain.repository.GeocodingRepository
import com.routecj.customer.domain.repository.NotificationRepository
import com.routecj.customer.domain.repository.OrderRepository
import com.routecj.customer.domain.repository.RouteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

@HiltViewModel
@SuppressLint("MissingPermission")
class BookingViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val notificationRepository: NotificationRepository,
    private val geocodingRepository: GeocodingRepository,
    private val routeRepository: RouteRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private val _destinationState = MutableStateFlow<DestinationState>(DestinationState.Idle)
    val destinationState: StateFlow<DestinationState> = _destinationState.asStateFlow()

    private val _searchResults = MutableStateFlow<List<GeocodingResult>>(emptyList())
    val searchResults: StateFlow<List<GeocodingResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _bookingRoutePoints = MutableStateFlow<List<Pair<Double, Double>>>(emptyList())
    val bookingRoutePoints: StateFlow<List<Pair<Double, Double>>> = _bookingRoutePoints.asStateFlow()

    private val _packageState = MutableStateFlow(PackageState())
    val packageState: StateFlow<PackageState> = _packageState.asStateFlow()

    private val _scheduleState = MutableStateFlow(ScheduleState())
    val scheduleState: StateFlow<ScheduleState> = _scheduleState.asStateFlow()

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Draft)
    val bookingState: StateFlow<BookingState> = _bookingState.asStateFlow()

    fun requestLocation() {
        _locationState.value = LocationState.RequestingPermission
    }

    fun onPermissionGranted() {
        _locationState.value = LocationState.Loading
        fetchCurrentLocation()
    }

    fun onPermissionDenied() {
        _locationState.value = LocationState.Error("Location permission was denied. We need your location to set the pickup point.")
    }

    fun onPermissionPermanentlyDenied() {
        _locationState.value = LocationState.Error("Location permission is permanently denied. Please enable it in Settings.")
    }

    private fun fetchCurrentLocation() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation
                    if (location != null) {
                        handleLocationResult(location)
                        fusedLocationClient.removeLocationUpdates(this)
                    } else {
                        _locationState.value = LocationState.Error("Unable to retrieve location.")
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            _locationState.value = LocationState.Error("Error accessing location: ${e.message}")
        }
    }

    private fun handleLocationResult(location: Location) {
        if (location.latitude < -90 || location.latitude > 90 || location.longitude < -180 || location.longitude > 180) {
            _locationState.value = LocationState.Error("Received invalid GPS coordinates.")
            return
        }

        viewModelScope.launch {
            val address = getAddressFromCoordinates(location.latitude, location.longitude)
            _locationState.value = LocationState.Success(location.latitude, location.longitude, address)
        }
    }

    fun updatePickupLocation(lat: Double, lng: Double) {
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return

        _locationState.value = LocationState.Success(lat, lng, "Updating address...")

        viewModelScope.launch {
            val address = getAddressFromCoordinates(lat, lng)
            _locationState.value = LocationState.Success(lat, lng, address)

            val dest = _destinationState.value
            if (dest is DestinationState.Success) {
                fetchBookingRoute(lat, lng, dest.latitude, dest.longitude)
            }
        }
    }

    private suspend fun getAddressFromCoordinates(lat: Double, lng: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    address.getAddressLine(0)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun searchDestination(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchError.value = null
            return
        }

        _isSearching.value = true
        _searchError.value = null

        viewModelScope.launch {
            val result = geocodingRepository.searchAddress(query)
            result.onSuccess { list ->
                _isSearching.value = false
                _searchResults.value = list
                _searchError.value = null
            }.onFailure { error ->
                _isSearching.value = false
                _searchResults.value = emptyList()
                _searchError.value = error.message ?: "Couldn't find that location"
            }
        }
    }

    fun selectDestination(result: GeocodingResult) {
        _destinationState.value = DestinationState.Success(result.latitude, result.longitude, result.displayName)
        _searchResults.value = emptyList()

        // Fetch OSRM route if pickup location is available
        val pickup = _locationState.value
        if (pickup is LocationState.Success) {
            fetchBookingRoute(pickup.latitude, pickup.longitude, result.latitude, result.longitude)
        }
    }

    fun setDestination(latitude: Double, longitude: Double, address: String?) {
        _destinationState.value = DestinationState.Success(latitude, longitude, address)
        val pickup = _locationState.value
        if (pickup is LocationState.Success) {
            fetchBookingRoute(pickup.latitude, pickup.longitude, latitude, longitude)
        }
    }

    private fun fetchBookingRoute(originLat: Double, originLng: Double, destLat: Double, destLng: Double) {
        viewModelScope.launch {
            val result = routeRepository.getRoute(originLat, originLng, destLat, destLng)
            result.onSuccess { points ->
                _bookingRoutePoints.value = points
            }.onFailure {
                _bookingRoutePoints.value = emptyList()
            }
        }
    }

    fun updatePackage(
        packageType: String = _packageState.value.packageType ?: "",
        itemDescription: String = _packageState.value.itemDescription ?: "",
        packageCount: String = _packageState.value.packageCount?.toString() ?: "",
        weight: String = _packageState.value.weight?.toString() ?: "",
        specialInstructions: String = _packageState.value.specialInstructions ?: ""
    ) {
        val countInt = packageCount.toIntOrNull()
        val weightDouble = weight.toDoubleOrNull()

        _packageState.value = _packageState.value.copy(
            packageType = packageType.ifBlank { null },
            itemDescription = itemDescription.ifBlank { null },
            packageCount = countInt,
            weight = weightDouble,
            specialInstructions = specialInstructions.ifBlank { null }
        )
    }

    fun updateSchedule(date: String, timeSlot: String) {
        _scheduleState.value = _scheduleState.value.copy(date = date, timeSlot = timeSlot)
    }

    fun confirmBooking() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _bookingState.value = BookingState.Error("You must be logged in to create a booking.")
            return
        }

        val currentLocationState = _locationState.value
        if (currentLocationState !is LocationState.Success) {
            _bookingState.value = BookingState.Error("Pickup location is not confirmed. Please secure a location first.")
            return
        }

        val destState = _destinationState.value
        if (destState !is DestinationState.Success) {
            _bookingState.value = BookingState.Error("Destination is not set.")
            return
        }

        val pkgState = _packageState.value
        if (!pkgState.isValid) {
            _bookingState.value = BookingState.Error("Package details are incomplete or invalid.")
            return
        }

        val schedState = _scheduleState.value
        if (!schedState.isValid) {
            _bookingState.value = BookingState.Error("Pickup schedule is incomplete.")
            return
        }

        _bookingState.value = BookingState.Creating

        val orderId = UUID.randomUUID().toString()
        val order = Order(
            id = orderId,
            customerId = currentUser.uid,
            pickupLatitude = currentLocationState.latitude,
            pickupLongitude = currentLocationState.longitude,
            pickupAddress = currentLocationState.address,
            destinationLatitude = destState.latitude,
            destinationLongitude = destState.longitude,
            destinationAddress = destState.address,
            packageType = pkgState.packageType,
            itemDescription = pkgState.itemDescription,
            packageCount = pkgState.packageCount,
            weight = pkgState.weight,
            specialInstructions = pkgState.specialInstructions,
            pickupDate = schedState.date,
            pickupSlot = schedState.timeSlot,
            status = OrderStatus.BOOKED,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            val result = orderRepository.createOrder(order)
            result.onSuccess {
                notificationRepository.saveNotification(
                    com.routecj.customer.domain.model.CustomerNotification(
                        customerId = currentUser.uid,
                        orderId = orderId,
                        title = "Booking Confirmed",
                        message = "Your delivery booking has been created successfully.",
                        type = com.routecj.customer.domain.model.NotificationType.BOOKING_CREATED.name,
                        createdAt = System.currentTimeMillis(),
                        read = false
                    )
                )
                _bookingState.value = BookingState.Success(orderId)
            }.onFailure { error ->
                _bookingState.value = BookingState.Error(error.message ?: "Failed to create booking.")
            }
        }
    }

    fun retryBooking() {
        _bookingState.value = BookingState.Draft
    }
}
