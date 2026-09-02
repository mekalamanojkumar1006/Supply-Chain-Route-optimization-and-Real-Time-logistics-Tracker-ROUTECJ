package com.routecj.customer.domain.repository

interface OtpRepository {
    suspend fun generatePickupOtp(orderId: String): Result<String>
    suspend fun getSecureOtp(orderId: String): Result<String>
}
