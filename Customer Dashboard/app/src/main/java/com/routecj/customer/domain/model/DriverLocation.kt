package com.routecj.customer.domain.model

data class DriverLocation(
    val latitude: Double,
    val longitude: Double,
    val heading: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
