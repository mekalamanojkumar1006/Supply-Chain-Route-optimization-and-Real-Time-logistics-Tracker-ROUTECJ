package com.routecj.customer.domain.config

import java.util.Locale

/**
 * Centralized RouteCJ Payment & UPI Configuration.
 * Single source of truth for payee identity, UPI VPA, and URI payload generation.
 */
object RouteCJPaymentConfig {
    const val PAYEE_UPI_ID = "chinnujunnu@slc"
    const val PAYEE_NAME = "RouteCJ"
    const val CURRENCY = "INR"

    /**
     * Dynamically generates the UPI payment URI conforming to the official NPCI spec:
     * upi://pay?pa=chinnujunnu@slc&pn=RouteCJ&am=<ORDER_AMOUNT>&cu=INR
     *
     * @param amount Real order amount from the Order model formatted to 2 decimal places.
     */
    fun generateUpiUri(amount: Double): String {
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        return "upi://pay?pa=$PAYEE_UPI_ID&pn=$PAYEE_NAME&am=$formattedAmount&cu=$CURRENCY"
    }
}
