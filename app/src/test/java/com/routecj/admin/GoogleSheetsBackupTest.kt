package com.routecj.admin

import com.routecj.admin.domain.model.BackupHealthState
import com.routecj.admin.domain.model.BackupStatus
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class GoogleSheetsBackupTest {

    @Test
    fun testBackupHealthState_ConnectedWhenRecent() {
        val now = Date()
        val status = BackupStatus(
            status = BackupHealthState.CONNECTED,
            lastSuccessfulBackup = now,
            errorMessage = null
        )
        assertEquals(BackupHealthState.CONNECTED, status.effectiveState)
    }

    @Test
    fun testBackupHealthState_DelayedWhenOld() {
        val fortyMinutesAgo = Date(System.currentTimeMillis() - 45 * 60 * 1000)
        val status = BackupStatus(
            status = BackupHealthState.CONNECTED,
            lastSuccessfulBackup = fortyMinutesAgo,
            errorMessage = null
        )
        assertEquals(BackupHealthState.DELAYED, status.effectiveState)
    }

    @Test
    fun testBackupHealthState_ErrorWhenErrorMessagePresent() {
        val now = Date()
        val status = BackupStatus(
            status = BackupHealthState.CONNECTED,
            lastSuccessfulBackup = now,
            errorMessage = "Google Apps Script quota exceeded"
        )
        assertEquals(BackupHealthState.ERROR, status.effectiveState)
    }

    @Test
    fun testBackupHealthState_DelayedWhenNeverBackedUp() {
        val status = BackupStatus(
            status = BackupHealthState.CONNECTED,
            lastSuccessfulBackup = null,
            errorMessage = null
        )
        assertEquals(BackupHealthState.DELAYED, status.effectiveState)
    }

    @Test
    fun testDocumentIdUniquenessMapping() {
        // Verify document ID indexing structure prevents duplicate rows
        val testOrders = listOf(
            mapOf("id" to "ORD101", "customer" to "Alice", "status" to "PENDING"),
            mapOf("id" to "ORD102", "customer" to "Bob", "status" to "IN_TRANSIT"),
            mapOf("id" to "ORD101", "customer" to "Alice Updated", "status" to "DELIVERED") // update to ORD101
        )

        val sheetIndexMap = mutableMapOf<String, Int>()
        val sheetRows = mutableListOf<Map<String, Any>>()

        for (order in testOrders) {
            val docId = order["id"] as String
            val existingIndex = sheetIndexMap[docId]
            if (existingIndex != null) {
                sheetRows[existingIndex] = order // In-place row update
            } else {
                val newIndex = sheetRows.size
                sheetRows.add(order) // In-place row insert
                sheetIndexMap[docId] = newIndex
            }
        }

        // Must only have 2 rows (ORD101 and ORD102), never 3 rows
        assertEquals(2, sheetRows.size)
        assertEquals("DELIVERED", sheetRows[0]["status"])
        assertEquals("Alice Updated", sheetRows[0]["customer"])
        assertEquals("IN_TRANSIT", sheetRows[1]["status"])
    }

    @Test
    fun testLatitudeAndLongitudeRangeValidation() {
        fun isValidCoordinate(lat: Double, lon: Double): Boolean {
            return lat in -90.0..90.0 && lon in -180.0..180.0
        }

        // Valid Vizianagaram Godown Coordinates
        assertTrue(isValidCoordinate(18.090100, 83.431239))
        // Valid Boundary Coordinates
        assertTrue(isValidCoordinate(90.0, 180.0))
        assertTrue(isValidCoordinate(-90.0, -180.0))
        assertTrue(isValidCoordinate(0.0, 0.0))

        // Invalid Latitude
        assertFalse(isValidCoordinate(95.0, 80.0))
        assertFalse(isValidCoordinate(-92.4, 45.0))

        // Invalid Longitude
        assertFalse(isValidCoordinate(20.0, 185.0))
        assertFalse(isValidCoordinate(20.0, -190.0))
    }

    @Test
    fun testPincodeValidation() {
        fun isValidPincode(pincode: String): Boolean {
            return pincode.matches(Regex("""^\d{6}$"""))
        }

        assertTrue(isValidPincode("535002"))
        assertTrue(isValidPincode("530001"))
        assertFalse(isValidPincode("53500")) // 5 digits
        assertFalse(isValidPincode("5350021")) // 7 digits
        assertFalse(isValidPincode("53500A")) // contains letter
        assertFalse(isValidPincode(""))
    }

    @Test
    fun testProtectedVsEditableFieldRules() {
        val protectedFields = setOf(
            "currentLatitude", "currentLongitude", "speed", "heading",
            "lastActive", "deliveryOtp", "verificationToken", "qrId",
            "uid", "adminId", "deliveredAt", "deliveredBy", "otpVerified"
        )

        val editableFields = setOf(
            "name", "phone", "address", "city", "state", "pincode",
            "latitude", "longitude", "capacity", "brand", "model",
            "registrationNumber", "vehicleType", "status", "remarks"
        )

        fun isFieldProtected(field: String): Boolean = protectedFields.contains(field)
        fun isFieldEditable(field: String): Boolean = editableFields.contains(field)

        assertTrue(isFieldProtected("currentLatitude"))
        assertTrue(isFieldProtected("deliveryOtp"))
        assertTrue(isFieldProtected("verificationToken"))
        assertFalse(isFieldProtected("registrationNumber"))

        assertTrue(isFieldEditable("latitude"))
        assertTrue(isFieldEditable("pincode"))
        assertTrue(isFieldEditable("phone"))
        assertFalse(isFieldEditable("deliveryOtp"))
    }

    @Test
    fun testLoopPreventionMetadataResolution() {
        val syncPayloadFromSheets = mapOf(
            "syncSource" to "GOOGLE_SHEETS",
            "updatedAt" to Date().toString()
        )

        assertEquals("GOOGLE_SHEETS", syncPayloadFromSheets["syncSource"])

        fun shouldTriggerOnEdit(source: String?, isLocked: Boolean): Boolean {
            if (isLocked) return false
            if (source == "FIREBASE_AUTOMATED_SYNC") return false
            return true
        }

        // Programmatic lock active -> Ignore onEdit
        assertFalse(shouldTriggerOnEdit("GOOGLE_SHEETS", isLocked = true))
        // Automated background sync -> Ignore onEdit
        assertFalse(shouldTriggerOnEdit("FIREBASE_AUTOMATED_SYNC", isLocked = false))
        // Legitimate user edit -> Trigger onEdit
        assertTrue(shouldTriggerOnEdit("USER_DIRECT_EDIT", isLocked = false))
    }
}
