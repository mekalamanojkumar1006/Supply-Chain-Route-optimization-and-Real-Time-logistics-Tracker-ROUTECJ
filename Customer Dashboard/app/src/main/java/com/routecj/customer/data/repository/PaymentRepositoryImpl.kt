package com.routecj.customer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.customer.domain.model.PaymentStatus
import com.routecj.customer.domain.repository.PaymentRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

/**
 * DEMO / SIMULATED PAYMENT ONLY.
 * This project intentionally does NOT integrate any real payment gateway.
 * No real financial transaction is performed. All transactions are clearly
 * labelled DEMO and stored as demonstration data only.
 */
class PaymentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : PaymentRepository {

    companion object {
        // Demo amount used when backend pricing is not yet configured
        const val DEMO_AMOUNT = 100.0
        const val DEMO_CURRENCY = "INR"
        const val DEMO_PAYMENT_MODE = "DEMO"
    }

    private val orders = firestore.collection("orders")

    override suspend fun initiatePayment(orderId: String, amount: Double): Result<String> {
        return try {
            // Generate a clearly labelled DEMO transaction reference
            val demoRef = "DEMO-TXN-${UUID.randomUUID().toString().uppercase().take(8)}"

            // Mark order as PROCESSING in Firestore
            orders.document(orderId).update(
                mapOf(
                    "paymentStatus" to PaymentStatus.PROCESSING.name,
                    "paymentMode" to DEMO_PAYMENT_MODE,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()

            Result.success(demoRef)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun recordPaymentResult(
        orderId: String,
        transactionId: String,
        status: PaymentStatus,
        amount: Double
    ): Result<Unit> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "paymentStatus" to status.name,
                "paymentMode" to DEMO_PAYMENT_MODE,
                "updatedAt" to System.currentTimeMillis()
            )

            if (status == PaymentStatus.SUCCESS) {
                updateData["transactionId"] = transactionId  // e.g. DEMO-TXN-XXXXXXXX
                updateData["paidAt"] = System.currentTimeMillis()
                updateData["amount"] = amount
                updateData["currency"] = DEMO_CURRENCY
                // Generate a clearly labelled DEMO invoice number
                val demoInvoiceNum = "DEMO-INV-${orderId.take(6).uppercase()}-${System.currentTimeMillis() / 1000}"
                updateData["invoiceNumber"] = demoInvoiceNum
            }

            orders.document(orderId).update(updateData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
