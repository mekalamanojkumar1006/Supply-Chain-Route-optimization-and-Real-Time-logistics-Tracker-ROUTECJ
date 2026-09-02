package com.routecj.customer.domain.config

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteCJPaymentConfigTest {

    @Test
    fun testUpiConfigConstants() {
        assertEquals("chinnujunnu@slc", RouteCJPaymentConfig.PAYEE_UPI_ID)
        assertEquals("RouteCJ", RouteCJPaymentConfig.PAYEE_NAME)
        assertEquals("INR", RouteCJPaymentConfig.CURRENCY)
    }

    @Test
    fun testGenerateUpiUri() {
        val uri499 = RouteCJPaymentConfig.generateUpiUri(499.0)
        assertEquals("upi://pay?pa=chinnujunnu@slc&pn=RouteCJ&am=499.00&cu=INR", uri499)

        val uri1250 = RouteCJPaymentConfig.generateUpiUri(1250.5)
        assertEquals("upi://pay?pa=chinnujunnu@slc&pn=RouteCJ&am=1250.50&cu=INR", uri1250)
    }
}
