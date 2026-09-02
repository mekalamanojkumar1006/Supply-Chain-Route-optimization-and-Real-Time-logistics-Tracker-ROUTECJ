package com.routecj.customer.domain.repository

import com.routecj.customer.domain.model.PaymentStatus

interface PaymentRepository {
    /**
     * Initiates a payment for the given order.
     * Returns a payment reference ID on success.
     * 🟡 PARTIAL / DEPENDENCY — Real gateway credentials (Razorpay/PayU/Stripe) must be configured.
     */
    suspend fun initiatePayment(orderId: String, amount: Double): Result<String>

    /**
     * Records the result of a payment attempt into Firestore.
     * Customer can only call this with SUCCESS after receiving gateway callback.
     * In production, this should be triggered by a server-side webhook, not the client.
     */
    suspend fun recordPaymentResult(
        orderId: String,
        transactionId: String,
        status: PaymentStatus,
        amount: Double
    ): Result<Unit>
}
