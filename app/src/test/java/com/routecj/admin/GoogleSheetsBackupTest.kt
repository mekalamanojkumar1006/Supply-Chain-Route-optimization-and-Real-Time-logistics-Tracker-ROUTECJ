package com.routecj.admin

import com.routecj.admin.domain.model.BackupHealthState
import com.routecj.admin.domain.model.BackupStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
