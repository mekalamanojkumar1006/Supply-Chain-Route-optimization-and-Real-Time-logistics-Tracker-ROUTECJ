package com.routecj.admin.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Extension functions for String operations.
 * Provides utility functions for common string manipulations.
 */

/**
 * Validates if the string is a valid email format.
 */
fun String.isValidEmail(): Boolean {
    return this.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

/**
 * Validates if the string is a valid phone number (basic validation).
 */
fun String.isValidPhoneNumber(): Boolean {
    return this.isNotEmpty() && this.length >= 10 && this.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
}

/**
 * Capitalizes the first letter of the string.
 */
fun String.capitalizeFirstLetter(): String {
    return if (this.isNotEmpty()) this[0].uppercase() + this.substring(1) else this
}

/**
 * Extension functions for Date operations.
 */

/**
 * Formats a Date object to a readable string format.
 */
fun Date.formatToString(format: String = "dd/MM/yyyy HH:mm"): String {
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    return sdf.format(this)
}

/**
 * Converts milliseconds to a Date object.
 */
fun Long.toDate(): Date {
    return Date(this)
}

/**
 * Extension functions for Number operations.
 */

/**
 * Formats a double to a currency string.
 */
fun Double.formatCurrency(currencySymbol: String = "₹"): String {
    return "$currencySymbol%.2f".format(this)
}

/**
 * Rounds a double to a specific number of decimal places.
 */
fun Double.roundTo(decimals: Int): Double {
    val multiplier = Math.pow(10.0, decimals.toDouble())
    return kotlin.math.round(this * multiplier) / multiplier
}

