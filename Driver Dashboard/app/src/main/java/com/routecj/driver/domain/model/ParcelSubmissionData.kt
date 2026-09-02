package com.routecj.driver.domain.model

/**
 * Domain model representing parcel input collected from the Driver
 * upon successful OTP verification.
 */
data class ParcelSubmissionData(
    val itemDescription: String,
    val packageCount: Int,
    val weight: Double,
    val specialInstructions: String = ""
)
