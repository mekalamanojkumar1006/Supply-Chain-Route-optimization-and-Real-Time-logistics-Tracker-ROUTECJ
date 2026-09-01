package com.routecj.admin.domain.model

import java.util.Date

/**
 * Domain model representing the health status of the Google Sheets backup system.
 */
data class BackupStatus(
    val status: BackupHealthState = BackupHealthState.CONNECTED,
    val lastSuccessfulBackup: Date? = null,
    val lastAttempt: Date? = null,
    val lastSyncSummary: String = "",
    val errorMessage: String? = null,
    val spreadsheetUrl: String? = null,
    val isConfigured: Boolean = true
) {
    /**
     * Determines the effective health state based on timestamps and status flags.
     */
    val effectiveState: BackupHealthState
        get() {
            if (errorMessage != null && errorMessage.isNotBlank()) {
                return BackupHealthState.ERROR
            }
            if (lastSuccessfulBackup == null) {
                return BackupHealthState.DELAYED
            }
            val diffMinutes = (System.currentTimeMillis() - lastSuccessfulBackup.time) / (1000 * 60)
            return when {
                diffMinutes <= 30 -> BackupHealthState.CONNECTED
                diffMinutes <= 120 -> BackupHealthState.DELAYED
                else -> BackupHealthState.DELAYED
            }
        }
}

enum class BackupHealthState {
    CONNECTED,  // 🟢 Backup Connected
    DELAYED,    // 🟡 Backup Delayed
    ERROR       // 🔴 Backup Error
}
