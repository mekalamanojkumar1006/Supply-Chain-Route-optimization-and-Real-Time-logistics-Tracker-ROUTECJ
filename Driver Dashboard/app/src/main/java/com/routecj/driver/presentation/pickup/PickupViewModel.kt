package com.routecj.driver.presentation.pickup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.BookedPickup
import com.routecj.driver.domain.usecase.GetBookedSlotsUseCase
import com.routecj.driver.domain.usecase.GetPickupDetailsUseCase
import com.routecj.driver.domain.usecase.MarkDriverArrivedUseCase
import com.routecj.driver.domain.usecase.VerifyPickupOtpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface BookedSlotsUiState {
    object Loading : BookedSlotsUiState
    data class Success(val pickups: List<BookedPickup>) : BookedSlotsUiState
    data class Error(val message: String) : BookedSlotsUiState
}

sealed interface PickupDetailsUiState {
    object Loading : PickupDetailsUiState
    data class Success(
        val pickup: BookedPickup,
        val isArriving: Boolean = false,
        val isVerifyingOtp: Boolean = false,
        val otpInput: String = "",
        val otpError: String? = null
    ) : PickupDetailsUiState
    data class Error(val message: String) : PickupDetailsUiState
    data class AccessDenied(val message: String) : PickupDetailsUiState
}

class PickupViewModel(
    private val getBookedSlotsUseCase: GetBookedSlotsUseCase,
    private val getPickupDetailsUseCase: GetPickupDetailsUseCase,
    private val markDriverArrivedUseCase: MarkDriverArrivedUseCase,
    private val verifyPickupOtpUseCase: VerifyPickupOtpUseCase
) : ViewModel() {

    private val _slotsState = MutableStateFlow<BookedSlotsUiState>(BookedSlotsUiState.Loading)
    val slotsState: StateFlow<BookedSlotsUiState> = _slotsState.asStateFlow()

    private val _detailsState = MutableStateFlow<PickupDetailsUiState>(PickupDetailsUiState.Loading)
    val detailsState: StateFlow<PickupDetailsUiState> = _detailsState.asStateFlow()

    private var currentDriverId: String = ""
    private var currentOrderId: String = ""

    fun loadBookedSlots(driverId: String) {
        currentDriverId = driverId
        viewModelScope.launch {
            _slotsState.value = BookedSlotsUiState.Loading
            getBookedSlotsUseCase(driverId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _slotsState.value = BookedSlotsUiState.Success(result.data)
                    }
                    is Result.Error -> {
                        _slotsState.value = BookedSlotsUiState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _slotsState.value = BookedSlotsUiState.Loading
                    }
                }
            }
        }
    }

    fun loadPickupDetails(orderId: String, driverId: String) {
        currentOrderId = orderId
        currentDriverId = driverId

        viewModelScope.launch {
            _detailsState.value = PickupDetailsUiState.Loading
            getPickupDetailsUseCase(orderId, driverId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        val current = _detailsState.value
                        val existingInput = if (current is PickupDetailsUiState.Success) current.otpInput else ""
                        val existingError = if (current is PickupDetailsUiState.Success) current.otpError else null

                        _detailsState.value = PickupDetailsUiState.Success(
                            pickup = result.data,
                            otpInput = existingInput,
                            otpError = existingError
                        )
                    }
                    is Result.Error -> {
                        if (result.message.contains("ACCESS DENIED", ignoreCase = true)) {
                            _detailsState.value = PickupDetailsUiState.AccessDenied(result.message)
                        } else {
                            _detailsState.value = PickupDetailsUiState.Error(result.message)
                        }
                    }
                    is Result.Loading -> {
                        _detailsState.value = PickupDetailsUiState.Loading
                    }
                }
            }
        }
    }

    fun onOtpInputChange(input: String) {
        val filtered = input.filter { it.isDigit() }.take(6)
        val currentState = _detailsState.value
        if (currentState is PickupDetailsUiState.Success) {
            _detailsState.value = currentState.copy(otpInput = filtered, otpError = null)
        }
    }

    fun markArrived() {
        val currentState = _detailsState.value
        if (currentState !is PickupDetailsUiState.Success) return
        if (currentState.isArriving || currentState.isVerifyingOtp) return

        viewModelScope.launch {
            _detailsState.value = currentState.copy(isArriving = true)
            val result = markDriverArrivedUseCase(currentOrderId, currentDriverId)
            if (result is Result.Error) {
                _detailsState.value = currentState.copy(
                    isArriving = false,
                    otpError = result.message
                )
            }
        }
    }

    fun verifyOtp() {
        val currentState = _detailsState.value
        if (currentState !is PickupDetailsUiState.Success) return
        if (currentState.isArriving || currentState.isVerifyingOtp) return

        val enteredOtp = currentState.otpInput.trim()
        if (enteredOtp.length < 4) {
            _detailsState.value = currentState.copy(otpError = "Please enter the complete OTP")
            return
        }

        viewModelScope.launch {
            _detailsState.value = currentState.copy(isVerifyingOtp = true, otpError = null)
            val result = verifyPickupOtpUseCase(currentOrderId, enteredOtp, currentDriverId)
            if (result is Result.Error) {
                _detailsState.value = currentState.copy(
                    isVerifyingOtp = false,
                    otpError = result.message
                )
            } else {
                _detailsState.value = currentState.copy(
                    isVerifyingOtp = false,
                    otpError = null
                )
            }
        }
    }

    fun retrySlots() {
        if (currentDriverId.isNotBlank()) loadBookedSlots(currentDriverId)
    }

    fun retryDetails() {
        if (currentOrderId.isNotBlank() && currentDriverId.isNotBlank()) {
            loadPickupDetails(currentOrderId, currentDriverId)
        }
    }
}
