package com.routecj.driver.presentation.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.TripDetails
import com.routecj.driver.domain.usecase.GetTripDetailsUseCase
import com.routecj.driver.domain.usecase.StartTripUseCase
import com.routecj.driver.domain.usecase.CompleteTripUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import com.routecj.driver.service.DriverGpsState
import com.routecj.driver.service.DriverLocationStateHolder

sealed interface TripDetailsUiState {
    object Loading : TripDetailsUiState
    data class Success(
        val trip: TripDetails,
        val isActionInProgress: Boolean = false,
        val errorMessage: String? = null
    ) : TripDetailsUiState
    data class Error(val message: String) : TripDetailsUiState
    data class AccessDenied(val message: String) : TripDetailsUiState
}

class TripViewModel(
    private val getTripDetailsUseCase: GetTripDetailsUseCase,
    private val startTripUseCase: StartTripUseCase,
    private val completeTripUseCase: CompleteTripUseCase,
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<TripDetailsUiState>(TripDetailsUiState.Loading)
    val uiState: StateFlow<TripDetailsUiState> = _uiState.asStateFlow()

    private var currentTripId: String = ""
    private var currentDriverId: String = ""
    private var observationJob: kotlinx.coroutines.Job? = null

    fun loadTrip(tripId: String, driverId: String) {
        currentTripId = tripId
        currentDriverId = driverId

        observationJob?.cancel()
        observationJob = scope.launch {
            _uiState.value = TripDetailsUiState.Loading
            getTripDetailsUseCase(tripId, driverId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = TripDetailsUiState.Success(result.data)
                    }
                    is Result.Error -> {
                        if (result.message.contains("ACCESS DENIED", ignoreCase = true)) {
                            _uiState.value = TripDetailsUiState.AccessDenied(result.message)
                        } else {
                            _uiState.value = TripDetailsUiState.Error(result.message)
                        }
                    }
                    is Result.Loading -> {
                        _uiState.value = TripDetailsUiState.Loading
                    }
                }
            }
        }
    }

    fun retry() {
        if (currentTripId.isNotBlank() && currentDriverId.isNotBlank()) {
            loadTrip(currentTripId, currentDriverId)
        }
    }

    fun startTrip() {
        val currentState = _uiState.value
        if (currentState !is TripDetailsUiState.Success) return
        if (currentState.isActionInProgress) return

        val trip = currentState.trip
        scope.launch {
            _uiState.value = currentState.copy(isActionInProgress = true)
            val startResult = startTripUseCase(
                tripId = trip.tripId,
                driverId = currentDriverId,
                isDispatchRecord = trip.isDispatchRecord
            )
            _uiState.update { state ->
                if (state is TripDetailsUiState.Success) {
                    if (startResult is Result.Error) {
                        state.copy(
                            isActionInProgress = false,
                            errorMessage = mapErrorMessage(startResult.message)
                        )
                    } else {
                        state.copy(isActionInProgress = false)
                    }
                } else state
            }
        }
    }

    fun completeTrip() {
        val currentState = _uiState.value
        if (currentState !is TripDetailsUiState.Success) return
        if (currentState.isActionInProgress) return

        val trip = currentState.trip
        val gpsState = DriverLocationStateHolder.gpsState.value

        if (trip.driverId != currentDriverId) {
             _uiState.update { state -> if (state is TripDetailsUiState.Success) state.copy(errorMessage = "TRIP NOT READY: Unauthorized driver.") else state }
             return
        }

        if (trip.status == "DELIVERED") {
             _uiState.update { state -> if (state is TripDetailsUiState.Success) state.copy(errorMessage = "DELIVERY ALREADY COMPLETED: This delivery has already been completed.") else state }
             return
        }

        if (trip.status == "CANCELLED") {
             _uiState.update { state -> if (state is TripDetailsUiState.Success) state.copy(errorMessage = "TRIP CANCELLED: This trip has been cancelled.") else state }
             return
        }

        if (trip.status !in listOf("DISPATCHED", "IN_TRANSIT")) {
             _uiState.update { state -> if (state is TripDetailsUiState.Success) state.copy(errorMessage = "TRIP NOT READY: This trip is not active.") else state }
             return
        }

        if (!trip.otpVerified && !trip.isDispatchRecord) {
             _uiState.update { state -> if (state is TripDetailsUiState.Success) state.copy(errorMessage = "TRIP NOT READY: Pickup workflow must be completed first.") else state }
             return
        }

        if (gpsState !is DriverGpsState.Active || gpsState.isOffline) {
             _uiState.update { state -> if (state is TripDetailsUiState.Success) state.copy(errorMessage = "GPS ERROR: Unable to obtain your current location.") else state }
             return
        }

        val dest = GeoPoint(trip.destinationLat, trip.destinationLng)
        val driverLoc = GeoPoint(gpsState.latitude, gpsState.longitude)
        val distance = driverLoc.distanceToAsDouble(dest)

        if (distance > 50.0 || gpsState.accuracy > 100f) {
             _uiState.update { state -> if (state is TripDetailsUiState.Success) state.copy(errorMessage = "DELIVERY NOT READY: Please move closer to the destination.") else state }
             return
        }

        scope.launch {
            _uiState.value = currentState.copy(isActionInProgress = true)
            val completeResult = completeTripUseCase(
                tripId = trip.tripId,
                driverId = currentDriverId,
                isDispatchRecord = trip.isDispatchRecord
            )
            _uiState.update { state ->
                if (state is TripDetailsUiState.Success) {
                    if (completeResult is Result.Error) {
                        state.copy(
                            isActionInProgress = false,
                            errorMessage = mapErrorMessage(completeResult.message)
                        )
                    } else {
                        state.copy(isActionInProgress = false)
                    }
                } else state
            }
        }
    }

    private fun mapErrorMessage(rawMessage: String): String {
        val msg = rawMessage.trim()
        return when {
            msg.contains("VEHICLE_MISMATCH") -> "VEHICLE MISMATCH: Order vehicle does not match driver's assigned vehicle."
            msg.contains("NO_VEHICLE_REFERENCE") || msg.contains("VEHICLE_NOT_ASSIGNED") -> "VEHICLE NOT ASSIGNED: You must have an assigned vehicle to start trip."
            msg.contains("TRIP_VEHICLE_NOT_ASSIGNED") -> "TRIP VEHICLE NOT ASSIGNED: This trip does not have an assigned vehicle."
            msg.contains("VEHICLE_RECORD_NOT_FOUND") || msg.contains("TRIP_VEHICLE_NOT_FOUND") -> "VEHICLE RECORD NOT FOUND: Vehicle record could not be found."
            msg.contains("UNAVAILABLE") -> "VEHICLE UNAVAILABLE: Vehicle is currently unavailable."
            msg.contains("OFFLINE") || msg.contains("network") -> "NO INTERNET CONNECTION: Please check your network."
            else -> msg
        }
    }

    fun clearErrorMessage() {
        val currentState = _uiState.value
        if (currentState is TripDetailsUiState.Success) {
            _uiState.value = currentState.copy(errorMessage = null)
        }
    }
}
