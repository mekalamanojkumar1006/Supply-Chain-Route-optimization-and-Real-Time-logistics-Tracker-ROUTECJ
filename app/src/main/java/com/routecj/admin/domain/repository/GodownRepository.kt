package com.routecj.admin.domain.repository

import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Godown
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Godown data.
 */
interface GodownRepository {
    /**
     * Get all godowns reactively.
     */
    suspend fun getAllGodowns(): Flow<Result<List<Godown>>>

    /**
     * Get a specific godown by ID.
     */
    suspend fun getGodownById(id: String): Result<Godown>

    /**
     * Create a new godown record.
     */
    suspend fun createGodown(godown: Godown): Result<Unit>

    /**
     * Update an existing godown record.
     */
    suspend fun updateGodown(godown: Godown): Result<Unit>

    /**
     * Delete a godown record.
     */
    suspend fun deleteGodown(id: String): Result<Unit>
}
