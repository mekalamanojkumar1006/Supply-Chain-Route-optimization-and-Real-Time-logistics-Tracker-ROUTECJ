package com.routecj.driver.presentation.parcel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.BookedPickup
import com.routecj.driver.domain.usecase.GetPickupDetailsUseCase
import com.routecj.driver.domain.usecase.SubmitParcelDetailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface ParcelUiState {
    object Loading : ParcelUiState
    data class OtpRequired(val message: String) : ParcelUiState
    data class AccessDenied(val message: String) : ParcelUiState
    data class AlreadySubmitted(val pickup: BookedPickup) : ParcelUiState
    data class FormReady(
        val pickup: BookedPickup,
        val itemDescription: String = "",
        val packageCount: String = "1",
        val weight: String = "",
        val specialInstructions: String = "",
        val itemDescriptionError: String? = null,
        val packageCountError: String? = null,
        val weightError: String? = null,
        val isSubmitting: Boolean = false,
        val generalError: String? = null
    ) : ParcelUiState
    data class Success(
        val pickup: BookedPickup,
        val itemDescription: String,
        val packageCount: Int,
        val weight: Double
    ) : ParcelUiState
    data class Error(val message: String) : ParcelUiState
}

class ParcelViewModel(
    private val getPickupDetailsUseCase: GetPickupDetailsUseCase,
    private val submitParcelDetailsUseCase: SubmitParcelDetailsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ParcelUiState>(ParcelUiState.Loading)
    val uiState: StateFlow<ParcelUiState> = _uiState.asStateFlow()

    private var currentOrderId: String = ""
    private var currentDriverId: String = ""

    fun initialize(orderId: String, driverId: String) {
        currentOrderId = orderId
        currentDriverId = driverId
        loadPickup(orderId, driverId)
    }

    fun loadPickup(orderId: String, driverId: String) {
        viewModelScope.launch {
            _uiState.value = ParcelUiState.Loading
            getPickupDetailsUseCase(orderId, driverId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        val pickup = result.data
                        when {
                            !pickup.otpVerified -> {
                                _uiState.value = ParcelUiState.OtpRequired(
                                    "Verify the customer pickup OTP before entering parcel details."
                                )
                            }
                            pickup.status.equals("PENDING_GODOWN_REVIEW", ignoreCase = true) -> {
                                _uiState.value = ParcelUiState.AlreadySubmitted(pickup)
                            }
                            else -> {
                                val current = _uiState.value
                                if (current is ParcelUiState.FormReady) {
                                    _uiState.value = current.copy(pickup = pickup)
                                } else if (current !is ParcelUiState.Success) {
                                    _uiState.value = ParcelUiState.FormReady(
                                        pickup = pickup,
                                        itemDescription = pickup.itemName.takeIf { it != "Freight Parcel" } ?: "",
                                        packageCount = if (pickup.quantity > 0) pickup.quantity.toString() else "1",
                                        weight = if (pickup.weight > 0) pickup.weight.toString() else "",
                                        specialInstructions = pickup.specialInstructions
                                    )
                                }
                            }
                        }
                    }
                    is Result.Error -> {
                        if (result.message.contains("ACCESS DENIED", ignoreCase = true)) {
                            _uiState.value = ParcelUiState.AccessDenied(result.message)
                        } else {
                            _uiState.value = ParcelUiState.Error(result.message)
                        }
                    }
                    is Result.Loading -> {
                        _uiState.value = ParcelUiState.Loading
                    }
                }
            }
        }
    }

    fun onItemDescriptionChange(value: String) {
        val current = _uiState.value as? ParcelUiState.FormReady ?: return
        _uiState.value = current.copy(itemDescription = value, itemDescriptionError = null, generalError = null)
    }

    fun onPackageCountChange(value: String) {
        val current = _uiState.value as? ParcelUiState.FormReady ?: return
        val filtered = value.filter { it.isDigit() }.take(4)
        _uiState.value = current.copy(packageCount = filtered, packageCountError = null, generalError = null)
    }

    fun onWeightChange(value: String) {
        val current = _uiState.value as? ParcelUiState.FormReady ?: return
        // Allow digits and single decimal point
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.value = current.copy(weight = filtered, weightError = null, generalError = null)
    }

    fun onSpecialInstructionsChange(value: String) {
        val current = _uiState.value as? ParcelUiState.FormReady ?: return
        _uiState.value = current.copy(specialInstructions = value)
    }

    fun submitParcel() {
        val current = _uiState.value as? ParcelUiState.FormReady ?: return
        if (current.isSubmitting) return

        // Local Validation
        var hasError = false
        var descError: String? = null
        var countError: String? = null
        var weightError: String? = null

        if (current.itemDescription.trim().isBlank()) {
            descError = "Enter item description."
            hasError = true
        }

        val count = current.packageCount.trim().toIntOrNull()
        if (count == null || count <= 0) {
            countError = "Package count must be at least 1."
            hasError = true
        }

        val weightVal = if (current.weight.isBlank()) 0.0 else current.weight.trim().toDoubleOrNull()
        if (weightVal == null || weightVal < 0.0) {
            weightError = "Enter a valid weight."
            hasError = true
        }

        if (hasError) {
            _uiState.value = current.copy(
                itemDescriptionError = descError,
                packageCountError = countError,
                weightError = weightError
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isSubmitting = true, generalError = null)
            val result = submitParcelDetailsUseCase(
                orderId = currentOrderId,
                driverId = currentDriverId,
                itemDescription = current.itemDescription,
                packageCountStr = current.packageCount,
                weightStr = current.weight,
                specialInstructions = current.specialInstructions
            )

            when (result) {
                is Result.Success -> {
                    _uiState.value = ParcelUiState.Success(
                        pickup = current.pickup,
                        itemDescription = current.itemDescription.trim(),
                        packageCount = count ?: 1,
                        weight = weightVal ?: 0.0
                    )
                }
                is Result.Error -> {
                    if (result.message.contains("ALREADY SUBMITTED", ignoreCase = true)) {
                        _uiState.value = ParcelUiState.AlreadySubmitted(current.pickup)
                    } else if (result.message.contains("VERIFICATION REQUIRED", ignoreCase = true)) {
                        _uiState.value = ParcelUiState.OtpRequired(result.message)
                    } else if (result.message.contains("ACCESS DENIED", ignoreCase = true)) {
                        _uiState.value = ParcelUiState.AccessDenied(result.message)
                    } else {
                        _uiState.value = current.copy(
                            isSubmitting = false,
                            generalError = result.message
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun retry() {
        if (currentOrderId.isNotBlank() && currentDriverId.isNotBlank()) {
            loadPickup(currentOrderId, currentDriverId)
        }
    }
}
