package com.routecj.driver.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

/**
 * Live GPS tracking state for the Driver application.
 */
sealed interface DriverGpsState {
    object Inactive : DriverGpsState
    object Connecting : DriverGpsState
    object PermissionRequired : DriverGpsState
    object LocationDisabled : DriverGpsState
    object OfflineWaiting : DriverGpsState
    object WaitingForSignal : DriverGpsState
    object StartFailed : DriverGpsState
    data class Active(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float,
        val speed: Float,
        val bearing: Float = 0f,
        val timestamp: Date,
        val tripId: String,
        val isOffline: Boolean = false,
        val isLastKnownLocation: Boolean = false
    ) : DriverGpsState
}

/**
 * Singleton state holder for observation of driver GPS updates across Compose UI and Services.
 */
object DriverLocationStateHolder {
    private val _gpsState = MutableStateFlow<DriverGpsState>(DriverGpsState.Inactive)
    val gpsState: StateFlow<DriverGpsState> = _gpsState.asStateFlow()

    fun updateState(state: DriverGpsState) {
        _gpsState.value = state
    }
}
