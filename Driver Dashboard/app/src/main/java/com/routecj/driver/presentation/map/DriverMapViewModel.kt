package com.routecj.driver.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.driver.core.routing.OsrmRouteResult
import com.routecj.driver.core.routing.OsrmRoutingClient
import com.routecj.driver.core.util.Result
import com.routecj.driver.data.repository.LocalStoreRepository
import com.routecj.driver.domain.model.StoreLocation
import com.routecj.driver.domain.model.TripDetails
import com.routecj.driver.domain.usecase.GetSelectedStoreUseCase
import com.routecj.driver.domain.usecase.GetStoreLocationsUseCase
import com.routecj.driver.domain.usecase.GetTripDetailsUseCase
import com.routecj.driver.service.DriverGpsState
import com.routecj.driver.service.DriverLocationStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

sealed interface DriverMapUiState {
    object Loading : DriverMapUiState
    data class Active(
        val tripDetails: TripDetails? = null,
        val driverLocation: GeoPoint? = null,
        val pickupLocation: GeoPoint? = null,
        val destinationLocation: GeoPoint? = null,
        val selectedStore: StoreLocation? = null,
        val availableStores: List<StoreLocation> = emptyList(),
        val targetPoint: GeoPoint? = null,
        val targetLabel: String = "",
        val routeResult: OsrmRouteResult? = null,
        val gpsState: DriverGpsState = DriverGpsState.Inactive,
        val distanceRemaining: Double? = null,
        val etaSeconds: Double? = null,
        val routeProgress: Int = 0,
        val isAtTarget: Boolean = false,
        val isRoutingLoading: Boolean = false,
        val isRoutingFailed: Boolean = false
    ) : DriverMapUiState
    data class Error(val message: String) : DriverMapUiState
}

class DriverMapViewModel(
    private val getTripDetailsUseCase: GetTripDetailsUseCase,
    private val getStoreLocationsUseCase: GetStoreLocationsUseCase = GetStoreLocationsUseCase(LocalStoreRepository()),
    private val getSelectedStoreUseCase: GetSelectedStoreUseCase = GetSelectedStoreUseCase(LocalStoreRepository()),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<DriverMapUiState>(DriverMapUiState.Loading)
    val uiState: StateFlow<DriverMapUiState> = _uiState.asStateFlow()

    private var currentTripDetails: TripDetails? = null
    private var currentDriverPoint: GeoPoint? = null
    private var currentSelectedStore: StoreLocation? = null
    private var currentStores: List<StoreLocation> = emptyList()

    private var lastCalculatedTargetPoint: GeoPoint? = null
    private var currentRouteResult: OsrmRouteResult? = null

    private var lastRouteRequestLocation: GeoPoint? = null
    private var initialTargetDistance: Double = 0.0
    private var isAtTarget: Boolean = false

    companion object {
        const val ARRIVAL_THRESHOLD_METERS = 50.0
        const val ROUTE_REFRESH_THRESHOLD_METERS = 100.0
    }

    private var tripObservationJob: Job? = null
    private var gpsObservationJob: Job? = null
    private var storeObservationJob: Job? = null
    private var routeJob: Job? = null

    fun initialize(tripId: String, driverId: String) {
        observeStores()
        observeTrip(tripId, driverId)
        observeGps()
    }

    fun selectStore(store: StoreLocation) {
        currentSelectedStore = store
        recomputeMapState()
    }

    fun setTripDetails(tripDetails: TripDetails?) {
        currentTripDetails = tripDetails
        recomputeMapState()
    }

    fun setStores(stores: List<StoreLocation>, selectedStore: StoreLocation? = null) {
        currentStores = stores
        currentSelectedStore = selectedStore ?: stores.firstOrNull { it.isActive }
        recomputeMapState()
    }

    private fun observeStores() {
        storeObservationJob?.cancel()
        storeObservationJob = scope.launch {
            getStoreLocationsUseCase().collectLatest { result ->
                if (result is Result.Success) {
                    currentStores = result.data
                    if (currentSelectedStore == null) {
                        currentSelectedStore = result.data.firstOrNull { it.isActive } ?: StoreLocation.VIZIANAGARAM_STORE
                    }
                    recomputeMapState()
                } else if (currentSelectedStore == null) {
                    currentSelectedStore = StoreLocation.VIZIANAGARAM_STORE
                    currentStores = listOf(StoreLocation.VIZIANAGARAM_STORE)
                    recomputeMapState()
                }
            }
        }
    }

    private fun observeTrip(tripId: String, driverId: String) {
        tripObservationJob?.cancel()
        if (tripId.isBlank()) {
            currentTripDetails = null
            recomputeMapState()
            return
        }

        tripObservationJob = scope.launch {
            getTripDetailsUseCase(tripId, driverId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        currentTripDetails = result.data
                        recomputeMapState()
                    }
                    is Result.Error -> {
                        currentTripDetails = null
                        recomputeMapState()
                    }
                    is Result.Loading -> {
                        if (currentTripDetails == null && currentSelectedStore == null) {
                            _uiState.value = DriverMapUiState.Loading
                        }
                    }
                }
            }
        }
    }

    private fun observeGps() {
        gpsObservationJob?.cancel()
        gpsObservationJob = scope.launch {
            DriverLocationStateHolder.gpsState.collectLatest { state ->
                if (state is DriverGpsState.Active) {
                    val newPoint = GeoPoint(state.latitude, state.longitude)
                    currentDriverPoint = newPoint
                }
                recomputeMapState()
            }
        }
    }

    fun recomputeMapState() {
        val trip = currentTripDetails
        val gpsState = DriverLocationStateHolder.gpsState.value

        if (gpsState is DriverGpsState.Active) {
            currentDriverPoint = GeoPoint(gpsState.latitude, gpsState.longitude)
        }

        val selectedStore = currentSelectedStore ?: StoreLocation.VIZIANAGARAM_STORE
        val availableStores = if (currentStores.isNotEmpty()) currentStores else listOf(selectedStore)

        val pickup = if (trip != null && trip.originLat != 0.0 && trip.originLng != 0.0) {
            GeoPoint(trip.originLat, trip.originLng)
        } else null

        val destination = if (trip != null && trip.destinationLat != 0.0 && trip.destinationLng != 0.0) {
            GeoPoint(trip.destinationLat, trip.destinationLng)
        } else null

        // Determine Customer Target based on pickup/OTP workflow state
        val isPickedUp = trip != null && (
            trip.otpVerified || trip.status.uppercase() in listOf("PICKED_UP", "READY_FOR_DISPATCH", "DISPATCHED", "IN_TRANSIT", "DELIVERED")
        )

        val (targetPoint, targetLabel) = when {
            trip != null -> {
                if (isPickedUp) {
                    val destPoint = destination ?: pickup
                    Pair(destPoint, "CUSTOMER DELIVERY")
                } else {
                    val pickPoint = pickup ?: destination
                    Pair(pickPoint, "CUSTOMER PICKUP")
                }
            }
            selectedStore != null -> Pair(GeoPoint(selectedStore.latitude, selectedStore.longitude), selectedStore.name)
            currentDriverPoint != null -> Pair(currentDriverPoint, "Driver Location")
            else -> Pair(null, "No Target Available")
        }

        val driverPoint = currentDriverPoint
        val routeResult = currentRouteResult

        if (driverPoint != null && targetPoint != null) {
            val distance = driverPoint.distanceToAsDouble(targetPoint)
            val accuracy = (gpsState as? DriverGpsState.Active)?.accuracy ?: 0f
            val isAccuracyAcceptable = accuracy <= 100f
            isAtTarget = distance <= ARRIVAL_THRESHOLD_METERS && isAccuracyAcceptable
        } else {
            isAtTarget = false
        }

        if (driverPoint != null && targetPoint != null && driverPoint != targetPoint) {
            val targetChanged = lastCalculatedTargetPoint == null || lastCalculatedTargetPoint != targetPoint

            var isOffRoute = false
            if (routeResult != null) {
                var minDistance = Double.MAX_VALUE
                for (point in routeResult.points) {
                    val dist = driverPoint.distanceToAsDouble(point)
                    if (dist < minDistance) minDistance = dist
                }
                isOffRoute = minDistance > 50.0
            }

            if (targetChanged || isOffRoute || routeResult == null) {
                calculateRoute(driverPoint, targetPoint)
            }
        }

        var currentDistanceRemaining: Double? = null
        var currentEtaSeconds: Double? = null
        var progress = 0

        if (routeResult != null && driverPoint != null && lastRouteRequestLocation != null) {
            val distSinceLastCall = driverPoint.distanceToAsDouble(lastRouteRequestLocation!!)
            val osrmDistance = routeResult.distanceMeters
            val remaining = (osrmDistance - distSinceLastCall).coerceAtLeast(0.0)
            currentDistanceRemaining = remaining

            val osrmEta = routeResult.durationSeconds
            if (osrmDistance > 0) {
                currentEtaSeconds = (remaining / osrmDistance) * osrmEta
            } else {
                currentEtaSeconds = 0.0
            }

            if (initialTargetDistance > 0) {
                progress = ((1.0 - (remaining / initialTargetDistance)) * 100).toInt().coerceIn(0, 100)
            }
        } else if (driverPoint != null && targetPoint != null) {
            val straightDist = driverPoint.distanceToAsDouble(targetPoint)
            currentDistanceRemaining = straightDist
            currentEtaSeconds = straightDist / 13.8
            if (initialTargetDistance > 0) {
                progress = ((1.0 - (straightDist / initialTargetDistance)) * 100).toInt().coerceIn(0, 100)
            }
        }

        _uiState.value = DriverMapUiState.Active(
            tripDetails = trip,
            driverLocation = currentDriverPoint,
            pickupLocation = pickup,
            destinationLocation = destination,
            selectedStore = selectedStore,
            availableStores = availableStores,
            targetPoint = targetPoint,
            targetLabel = targetLabel,
            routeResult = currentRouteResult,
            gpsState = gpsState,
            distanceRemaining = currentDistanceRemaining,
            etaSeconds = currentEtaSeconds,
            routeProgress = progress,
            isAtTarget = isAtTarget,
            isRoutingFailed = currentRouteResult == null && driverPoint != null && targetPoint != null
        )
    }

    private fun calculateRoute(start: GeoPoint, end: GeoPoint) {
        routeJob?.cancel()
        val isNewTarget = lastCalculatedTargetPoint != end
        lastCalculatedTargetPoint = end
        lastRouteRequestLocation = start

        routeJob = scope.launch {
            val route = OsrmRoutingClient.getDrivingRoute(start, end)
            if (route != null) {
                currentRouteResult = route
                if (isNewTarget || initialTargetDistance == 0.0) {
                    initialTargetDistance = route.distanceMeters
                }
                recomputeMapState()
            } else {
                currentRouteResult = null
                val currentState = _uiState.value
                if (currentState is DriverMapUiState.Active) {
                    _uiState.value = currentState.copy(
                        routeResult = null,
                        isRoutingFailed = true
                    )
                }
            }
        }
    }

    fun retry() {
        val driverPoint = currentDriverPoint
        val currentState = _uiState.value as? DriverMapUiState.Active
        val target = currentState?.targetPoint
        if (driverPoint != null && target != null) {
            calculateRoute(driverPoint, target)
        }
    }
}
