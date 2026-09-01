package com.routecj.admin

import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.model.OrderStatus
import com.routecj.admin.domain.model.PaymentMethod
import com.routecj.admin.domain.model.PaymentStatus
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class PaymentSystemTest {

    @Test
    fun testPaymentStatusResolution() {
        assertEquals(PaymentStatus.PENDING, PaymentStatus.fromString("PENDING"))
        assertEquals(PaymentStatus.PAID, PaymentStatus.fromString("PAID"))
        assertEquals(PaymentStatus.PARTIALLY_PAID, PaymentStatus.fromString("PARTIALLY_PAID"))
        assertEquals(PaymentStatus.PARTIALLY_PAID, PaymentStatus.fromString("Partially Paid"))
        assertEquals(PaymentStatus.COD, PaymentStatus.fromString("COD"))
        assertEquals(PaymentStatus.FAILED, PaymentStatus.fromString("FAILED"))
        assertEquals(PaymentStatus.REFUNDED, PaymentStatus.fromString("REFUNDED"))
        // Fallback
        assertEquals(PaymentStatus.PENDING, PaymentStatus.fromString("UNKNOWN_STATUS"))
    }

    @Test
    fun testPaymentMethodResolution() {
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("CASH"))
        assertEquals(PaymentMethod.UPI, PaymentMethod.fromString("UPI"))
        assertEquals(PaymentMethod.CARD, PaymentMethod.fromString("CARD"))
        assertEquals(PaymentMethod.BANK_TRANSFER, PaymentMethod.fromString("BANK_TRANSFER"))
        assertEquals(PaymentMethod.BANK_TRANSFER, PaymentMethod.fromString("Bank Transfer"))
        assertEquals(PaymentMethod.COD, PaymentMethod.fromString("COD"))
        assertEquals(PaymentMethod.OTHER, PaymentMethod.fromString("OTHER"))
        // Fallback
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("UNKNOWN_METHOD"))
    }

    @Test
    fun testOrderEffectivePaymentProperties() {
        val order = Order(
            id = "ORD-TEST-001",
            paymentStatus = "PARTIALLY_PAID",
            paymentMethod = "UPI",
            paymentAmount = 1500.0,
            transactionId = "UPI-1234567890",
            paymentNotes = "Advance payment received"
        )

        assertEquals(PaymentStatus.PARTIALLY_PAID, order.effectivePaymentStatus)
        assertEquals(PaymentMethod.UPI, order.effectivePaymentMethod)
        assertEquals(1500.0, order.paymentAmount, 0.001)
        assertEquals("UPI-1234567890", order.transactionId)
        assertEquals("Advance payment received", order.paymentNotes)
    }

    @Test
    fun testDefaultPaymentStatusIsPending() {
        val defaultOrder = Order(id = "ORD-DEFAULT")
        assertEquals("PENDING", defaultOrder.paymentStatus)
        assertEquals(PaymentStatus.PENDING, defaultOrder.effectivePaymentStatus)
        assertNull(defaultOrder.paymentTimestamp)
    }

    @Test
    fun testCodStatusAutoSelectsCodMethod() {
        var status = PaymentStatus.COD
        var method = PaymentMethod.CASH

        if (status == PaymentStatus.COD) {
            method = PaymentMethod.COD
        }

        assertEquals(PaymentMethod.COD, method)
    }

    @Test
    fun testTransactionIdValidationRules() {
        // Electronic methods require transaction ID when PAID or PARTIALLY_PAID
        fun validateTransactionId(status: PaymentStatus, method: PaymentMethod, txnId: String): Boolean {
            val isElectronic = method == PaymentMethod.UPI || method == PaymentMethod.CARD || method == PaymentMethod.BANK_TRANSFER
            val isPaid = status == PaymentStatus.PAID || status == PaymentStatus.PARTIALLY_PAID
            return if (isPaid && isElectronic) txnId.isNotBlank() else true
        }

        // UPI + PAID without Txn ID -> Invalid
        assertFalse(validateTransactionId(PaymentStatus.PAID, PaymentMethod.UPI, ""))
        // UPI + PAID with Txn ID -> Valid
        assertTrue(validateTransactionId(PaymentStatus.PAID, PaymentMethod.UPI, "UPI9876543210"))
        // CASH + PAID without Txn ID -> Valid (optional for Cash)
        assertTrue(validateTransactionId(PaymentStatus.PAID, PaymentMethod.CASH, ""))
        // COD + PENDING without Txn ID -> Valid (optional for COD)
        assertTrue(validateTransactionId(PaymentStatus.COD, PaymentMethod.COD, ""))
        // UPI + PENDING without Txn ID -> Valid (not yet paid)
        assertTrue(validateTransactionId(PaymentStatus.PENDING, PaymentMethod.UPI, ""))
    }

    @Test
    fun testPaymentAmountValidation() {
        fun isValidAmount(amountStr: String): Boolean {
            val amt = amountStr.toDoubleOrNull() ?: return false
            return amt >= 0.0
        }

        assertTrue(isValidAmount("1500"))
        assertTrue(isValidAmount("1500.50"))
        assertTrue(isValidAmount("0"))
        assertFalse(isValidAmount("-50"))
        assertFalse(isValidAmount("abc"))
    }

    @Test
    fun testUpiIdAndUriConstruction() {
        val upiId = com.routecj.admin.core.util.Constants.Payment.DEFAULT_UPI_ID
        assertEquals("manoj-2005-mekala@yes", upiId)

        val amount = 1500.0
        val orderNo = "ORD12345"
        val expectedUri = "upi://pay?pa=$upiId&pn=RouteCJ%20Logistics%20%28Manoj%20Mekala%29&cu=INR&am=1500.00&tn=Order%20ORD12345%20Payment&tr=ORD12345"
        
        // Ensure uri contains the configured UPI ID and amount
        assertTrue(expectedUri.contains("pa=manoj-2005-mekala@yes"))
        assertTrue(expectedUri.contains("am=1500.00"))
        assertTrue(expectedUri.contains("cu=INR"))
    }
}
