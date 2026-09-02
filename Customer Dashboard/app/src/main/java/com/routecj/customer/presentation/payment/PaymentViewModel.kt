package com.routecj.customer.presentation.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.customer.data.repository.PaymentRepositoryImpl
import com.routecj.customer.domain.model.PaymentStatus
import com.routecj.customer.domain.repository.InvoiceRepository
import com.routecj.customer.domain.repository.OrderRepository
import com.routecj.customer.domain.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val invoiceRepository: InvoiceRepository,
    private val notificationRepository: com.routecj.customer.domain.repository.NotificationRepository,
    private val authRepository: com.routecj.customer.domain.repository.AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    private val _paymentState = MutableStateFlow<PaymentUiState>(PaymentUiState.Loading)
    val paymentState: StateFlow<PaymentUiState> = _paymentState.asStateFlow()

    private val _invoiceState = MutableStateFlow<InvoiceUiState>(InvoiceUiState.Idle)
    val invoiceState: StateFlow<InvoiceUiState> = _invoiceState.asStateFlow()

    // Idempotency guard — prevents double-tap or recomposition retrigger
    private var paymentInFlight = false

    init {
        loadOrder()
    }

    private fun loadOrder() {
        viewModelScope.launch {
            orderRepository.getOrderFlow(orderId)
                .catch { e ->
                    _paymentState.value = PaymentUiState.Failure(
                        "Failed to load order: ${e.localizedMessage}", null
                    )
                }
                .collect { result ->
                    result.onSuccess { order ->
                        val existingStatus = PaymentStatus.fromString(order.paymentStatus)
                        when (existingStatus) {
                            PaymentStatus.SUCCESS -> {
                                // Demo payment already completed — go straight to success
                                _paymentState.value = PaymentUiState.Success(
                                    order = order,
                                    transactionId = order.transactionId ?: "DEMO-TXN-UNKNOWN",
                                    amount = order.totalAmount ?: order.amount ?: PaymentRepositoryImpl.DEMO_AMOUNT
                                )
                            }
                            PaymentStatus.PROCESSING -> {
                                _paymentState.value = PaymentUiState.Processing
                            }
                            else -> {
                                _paymentState.value = PaymentUiState.AwaitingPayment(order)
                            }
                        }
                    }.onFailure { e ->
                        _paymentState.value = PaymentUiState.Failure(
                            e.localizedMessage ?: "Unknown error", null
                        )
                    }
                }
        }
    }

    fun initiatePayment() {
        // Idempotency guard — block double-tap and recomposition re-trigger
        if (paymentInFlight) return
        val currentState = _paymentState.value
        if (currentState !is PaymentUiState.AwaitingPayment) return

        paymentInFlight = true
        val order = currentState.order
        // Use real totalAmount if set by backend, else fall back to DEMO amount
        val amount = order.totalAmount ?: PaymentRepositoryImpl.DEMO_AMOUNT

        viewModelScope.launch {
            _paymentState.value = PaymentUiState.Processing

            // Step 1: Create DEMO-TXN reference and mark PROCESSING in Firestore
            val initiateResult = paymentRepository.initiatePayment(orderId, amount)

            initiateResult.onSuccess { demoTxnRef ->
                // Step 2: Record DEMO SUCCESS (no real bank call — DEMO only)
                val recordResult = paymentRepository.recordPaymentResult(
                    orderId = orderId,
                    transactionId = demoTxnRef,   // e.g. DEMO-TXN-8F42A1XY
                    status = PaymentStatus.SUCCESS,
                    amount = amount
                )

                recordResult.onSuccess {
                    val uid = authRepository.getCurrentUserId()
                    if (uid != null) {
                        notificationRepository.saveNotification(
                            com.routecj.customer.domain.model.CustomerNotification(
                                customerId = uid,
                                orderId = orderId,
                                title = "Demo Payment Successful",
                                message = "Your demo payment for the shipment was completed.",
                                type = com.routecj.customer.domain.model.NotificationType.PAYMENT_SUCCESS.name,
                                createdAt = System.currentTimeMillis(),
                                read = false
                            )
                        )
                    }
                    // Firestore listener in loadOrder() will drive state to Success
                    // paymentInFlight stays true — prevents any re-entry
                }.onFailure { e ->
                    paymentInFlight = false
                    _paymentState.value = PaymentUiState.Failure(
                        "Demo payment processing error: ${e.localizedMessage}", order
                    )
                }
            }.onFailure { e ->
                paymentInFlight = false
                _paymentState.value = PaymentUiState.Failure(
                    "Demo payment initiation failed: ${e.localizedMessage}", order
                )
            }
        }
    }

    fun generateInvoice() {
        val currentState = _paymentState.value
        if (currentState !is PaymentUiState.Success) return
        if (_invoiceState.value is InvoiceUiState.Generating) return

        _invoiceState.value = InvoiceUiState.Generating
        viewModelScope.launch {
            val result = invoiceRepository.generateInvoice(currentState.order)
            result.onSuccess { file ->
                _invoiceState.value = InvoiceUiState.Ready(file)
            }.onFailure { e ->
                _invoiceState.value = InvoiceUiState.Error(
                    e.localizedMessage ?: "Failed to generate invoice"
                )
            }
        }
    }

    fun resetInvoiceError() {
        _invoiceState.value = InvoiceUiState.Idle
    }
}

// Extension to access amount field if stored separately
private val com.routecj.customer.domain.model.Order.amount: Double?
    get() = totalAmount
