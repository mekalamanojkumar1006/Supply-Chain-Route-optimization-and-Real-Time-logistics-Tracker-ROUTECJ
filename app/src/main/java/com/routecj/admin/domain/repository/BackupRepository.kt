package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.BackupStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for observing backup system health and triggering manual synchronization.
 */
interface BackupRepository {
    /**
     * Observes real-time backup system status from Firestore.
     */
    fun observeBackupStatus(): Flow<Result<BackupStatus>>

    /**
     * Triggers an on-demand synchronization request.
     */
    suspend fun triggerBackupSync(): Result<BackupStatus>

    /**
     * Updates the backup status metadata.
     */
    suspend fun updateBackupStatus(status: BackupStatus): Result<Unit>
}
