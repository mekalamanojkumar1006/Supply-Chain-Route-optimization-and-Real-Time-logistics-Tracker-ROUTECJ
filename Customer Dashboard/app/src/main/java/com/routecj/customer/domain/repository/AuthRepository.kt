package com.routecj.customer.domain.repository

import com.routecj.customer.domain.model.AuthResult

interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<AuthResult>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthResult>
    suspend fun signInWithGoogle(idToken: String): Result<AuthResult>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun deleteCurrentUser(): Result<Unit>
    fun getCurrentUserId(): String?
}
