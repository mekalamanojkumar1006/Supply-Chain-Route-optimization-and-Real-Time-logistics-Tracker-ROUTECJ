package com.routecj.customer.presentation.orders

import com.routecj.customer.domain.model.Order

sealed class OtpState {
    object Hidden : OtpState()
    object Loading : OtpState()
    data class Available(val otp: String, val expiresAt: Long) : OtpState()
    object Expired : OtpState()
    object Verified : OtpState()
    data class Error(val message: String) : OtpState()
}

sealed class OrderDetailsState {
    object Loading : OrderDetailsState()
    data class Success(val order: Order, val otpState: OtpState = OtpState.Hidden) : OrderDetailsState()
    data class Error(val message: String) : OrderDetailsState()
}
