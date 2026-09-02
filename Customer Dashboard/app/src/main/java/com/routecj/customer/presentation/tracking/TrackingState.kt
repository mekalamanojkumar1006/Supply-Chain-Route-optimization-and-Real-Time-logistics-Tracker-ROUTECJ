package com.routecj.customer.presentation.tracking

import com.routecj.customer.domain.model.DriverLocation

sealed class TrackingState {
    object Loading : TrackingState()
    object NoLocationYet : TrackingState()
    data class Active(val location: DriverLocation) : TrackingState()
    data class Stale(val location: DriverLocation) : TrackingState()
    data class Unavailable(val reason: String) : TrackingState()
}
