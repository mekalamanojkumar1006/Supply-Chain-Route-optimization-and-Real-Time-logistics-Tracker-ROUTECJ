package com.routecj.customer.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.customer.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.routecj.customer.domain.model.OrderStatus
import com.routecj.customer.domain.repository.OtpRepository

@HiltViewModel
class OrderDetailsViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val otpRepository: OtpRepository
) : ViewModel() {

    private val _state = MutableStateFlow<OrderDetailsState>(OrderDetailsState.Loading)
    val state: StateFlow<OrderDetailsState> = _state.asStateFlow()

    private var currentOrderId: String? = null
    private var isGeneratingOtp = false

    fun loadOrder(orderId: String) {
        currentOrderId = orderId
        viewModelScope.launch {
            orderRepository.getOrderFlow(orderId).collectLatest { result ->
                result.fold(
                    onSuccess = { order ->
                        val currentOtpState = (_state.value as? OrderDetailsState.Success)?.otpState ?: OtpState.Hidden
                        _state.value = OrderDetailsState.Success(order, currentOtpState)
                        
                        handleOtpLifecycle(order)
                    },
                    onFailure = { error ->
                        _state.value = OrderDetailsState.Error(error.message ?: "Failed to load order.")
                    }
                )
            }
        }
    }

    private fun handleOtpLifecycle(order: com.routecj.customer.domain.model.Order) {
        val now = System.currentTimeMillis()
        
        when (order.status) {
            OrderStatus.DRIVER_ARRIVED -> {
                when (order.pickupOtpStatus) {
                    "ACTIVE" -> {
                        val expiresAt = order.pickupOtpExpiresAt ?: 0L
                        if (now < expiresAt) {
                            // Active and unexpired, fetch it if we don't have it
                            val currentOtpState = (_state.value as? OrderDetailsState.Success)?.otpState
                            if (currentOtpState !is OtpState.Available) {
                                fetchExistingOtp(order.id, expiresAt)
                            }
                        } else {
                            // Expired
                            updateOtpState(OtpState.Expired)
                        }
                    }
                    null -> {
                        // Needs generation
                        generateOtp(order.id)
                    }
                }
            }
            OrderStatus.OTP_VERIFIED, OrderStatus.PARCEL_SUBMITTED, OrderStatus.IN_TRANSIT -> {
                updateOtpState(OtpState.Verified)
            }
            else -> {
                updateOtpState(OtpState.Hidden)
            }
        }
    }

    fun generateOtpForCurrentOrder() {
        val orderId = currentOrderId ?: return
        generateOtp(orderId)
    }

    private fun generateOtp(orderId: String) {
        if (isGeneratingOtp) return
        isGeneratingOtp = true
        updateOtpState(OtpState.Loading)
        
        viewModelScope.launch {
            otpRepository.generatePickupOtp(orderId).fold(
                onSuccess = { newOtp ->
                    // The Firestore listener will pick up the status change and update expiresAt,
                    // but we can proactively update UI if we want. We'll rely on the listener mostly,
                    // but since the listener doesn't pass the plaintext OTP, we must hold it in memory.
                    // When the listener fires, it will see "ACTIVE" and try to fetch it, which is fine,
                    // but we already have it here. Let's just set it.
                    val expiresAt = System.currentTimeMillis() + (5 * 60 * 1000)
                    updateOtpState(OtpState.Available(newOtp, expiresAt))
                    isGeneratingOtp = false
                },
                onFailure = { error ->
                    updateOtpState(OtpState.Error(error.message ?: "Failed to generate OTP"))
                    isGeneratingOtp = false
                }
            )
        }
    }

    private fun fetchExistingOtp(orderId: String, expiresAt: Long) {
        viewModelScope.launch {
            otpRepository.getSecureOtp(orderId).fold(
                onSuccess = { otp ->
                    updateOtpState(OtpState.Available(otp, expiresAt))
                },
                onFailure = {
                    updateOtpState(OtpState.Error("Could not retrieve active OTP"))
                }
            )
        }
    }

    private fun updateOtpState(otpState: OtpState) {
        val currentState = _state.value
        if (currentState is OrderDetailsState.Success) {
            _state.value = currentState.copy(otpState = otpState)
        }
    }
}
