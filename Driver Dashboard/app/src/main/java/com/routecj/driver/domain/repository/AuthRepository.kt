package com.routecj.driver.domain.repository

import com.google.firebase.auth.FirebaseUser
import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Driver
import kotlinx.coroutines.flow.Flow

/**
 * Authentication repository interface for Driver login, session management, and RBAC verification.
 */
interface AuthRepository {
    fun getCurrentFirebaseUser(): FirebaseUser?
    fun observeAuthState(): Flow<FirebaseUser?>
    suspend fun login(email: String, password: String): Result<Driver>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun getCurrentDriver(): Result<Driver>
    fun logout()
}
