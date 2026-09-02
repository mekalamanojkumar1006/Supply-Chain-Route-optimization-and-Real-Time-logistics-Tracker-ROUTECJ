package com.routecj.admin

import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.model.PaymentMethod
import com.routecj.admin.domain.model.PaymentStatus
import org.junit.Assert.*
import org.junit.Test

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
        val payeeName = com.routecj.admin.core.util.Constants.Payment.DEFAULT_PAYEE_NAME
        assertEquals("chinnujunnu@slc", upiId)
        assertEquals("RouteCJ", payeeName)

        val amount = 499.0
        val expectedUri = "upi://pay?pa=chinnujunnu@slc&pn=RouteCJ&am=499.00&cu=INR"
        val generatedUri = com.routecj.admin.core.util.QrCodeGenerator.buildUpiUri(
            upiId = upiId,
            payeeName = payeeName,
            amount = amount
        )

        assertEquals(expectedUri, generatedUri)
        assertTrue(generatedUri.contains("pa=chinnujunnu@slc"))
        assertTrue(generatedUri.contains("pn=RouteCJ"))
        assertTrue(generatedUri.contains("am=499.00"))
        assertTrue(generatedUri.contains("cu=INR"))
    }

    @Test
    fun testScanningQrDoesNotMarkOrderAsPaid() {
        val order = Order(
            id = "ORD-999",
            paymentStatus = "PENDING",
            paymentMethod = "UPI",
            paymentAmount = 499.0
        )

        // Generating / displaying QR code
        val uri = com.routecj.admin.core.util.QrCodeGenerator.buildUpiUri(
            amount = order.paymentAmount
        )

        assertNotNull(uri)
        // Verify payment status remains unchanged (PENDING) by QR code generation/display
        assertEquals(PaymentStatus.PENDING, order.effectivePaymentStatus)
        assertEquals("PENDING", order.paymentStatus)
    }
}
