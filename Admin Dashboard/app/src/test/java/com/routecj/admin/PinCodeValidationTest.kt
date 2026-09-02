package com.routecj.admin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PIN code validation in RouteCJ Admin parcel creation.
 */
class PinCodeValidationTest {

    private fun isValidPinCode(pin: String): Boolean {
        return pin.length == 6 && pin.all { it.isDigit() }
    }

    @Test
    fun validPinCodes_returnTrue() {
        assertTrue(isValidPinCode("500001"))
        assertTrue(isValidPinCode("530001"))
        assertTrue(isValidPinCode("110001"))
        assertTrue(isValidPinCode("600001"))
        assertTrue(isValidPinCode("400001"))
    }

    @Test
    fun invalidPinCodes_returnFalse() {
        assertFalse("Too short (3 digits)", isValidPinCode("123"))
        assertFalse("Too short (5 digits)", isValidPinCode("12345"))
        assertFalse("Too long (7 digits)", isValidPinCode("1234567"))
        assertFalse("Alphabetic", isValidPinCode("ABCDEF"))
        assertFalse("Alphanumeric", isValidPinCode("12345A"))
        assertFalse("Alphanumeric mixed", isValidPinCode("ABC123"))
        assertFalse("Blank", isValidPinCode(""))
        assertFalse("Contains spaces", isValidPinCode("500 01"))
    }
}
