package com.routecj.driver.domain.model

/**
 * Domain model representing a physical store / warehouse location.
 */
data class StoreLocation(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val isActive: Boolean = true
) {
    companion object {
        val VIZIANAGARAM_STORE = StoreLocation(
            id = "vizianagaram_store",
            name = "Vizianagaram Store",
            address = "APSRTC Bus Complex, Vizianagaram, Andhra Pradesh, India",
            latitude = 18.1085,
            longitude = 83.3988,
            isActive = true
        )
    }
}
