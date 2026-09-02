package com.routecj.customer.presentation.payment

import com.routecj.customer.domain.model.Order
import com.routecj.customer.domain.model.PaymentStatus
import java.io.File

sealed class PaymentUiState {
    object Loading : PaymentUiState()
    data class AwaitingPayment(val order: Order) : PaymentUiState()
    object Processing : PaymentUiState()
    data class Success(
        val order: Order,
        val transactionId: String,
        val amount: Double
    ) : PaymentUiState()
    data class Failure(val message: String, val order: Order?) : PaymentUiState()
}

sealed class InvoiceUiState {
    object Idle : InvoiceUiState()
    object Generating : InvoiceUiState()
    data class Ready(val file: File) : InvoiceUiState()
    data class Error(val message: String) : InvoiceUiState()
}
