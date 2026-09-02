package com.routecj.customer.presentation.booking

sealed class LocationState {
    object Idle : LocationState()
    object RequestingPermission : LocationState()
    object Loading : LocationState()
    data class Success(val latitude: Double, val longitude: Double, val address: String?) : LocationState()
    data class Error(val message: String) : LocationState()
}

sealed class DestinationState {
    object Idle : DestinationState()
    data class Success(val latitude: Double, val longitude: Double, val address: String?) : DestinationState()
}

data class PackageState(
    val packageType: String? = null,
    val itemDescription: String? = null,
    val packageCount: Int? = null,
    val weight: Double? = null,
    val specialInstructions: String? = null
) {
    val isValid: Boolean
        get() = packageType != null && packageCount != null && packageCount > 0 && weight != null && weight >= 0
}

data class ScheduleState(
    val date: String? = null,
    val timeSlot: String? = null
) {
    val isValid: Boolean
        get() = date != null && timeSlot != null
}

sealed class BookingState {
    object Draft : BookingState()
    object Creating : BookingState()
    data class Success(val orderId: String) : BookingState()
    data class Error(val message: String) : BookingState()
}
