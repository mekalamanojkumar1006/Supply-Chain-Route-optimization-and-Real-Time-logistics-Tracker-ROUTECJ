package com.routecj.customer.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.routecj.customer.core.error.toAuthError
import com.routecj.customer.domain.model.AuthResult
import com.routecj.customer.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User is null after sign in")
            Result.success(
                AuthResult(
                    uid = user.uid,
                    email = user.email,
                    isNewUser = result.additionalUserInfo?.isNewUser ?: false
                )
            )
        } catch (e: Exception) {
            Result.failure(e.toAuthError())
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): Result<AuthResult> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User is null after sign up")
            Result.success(
                AuthResult(
                    uid = user.uid,
                    email = user.email,
                    isNewUser = true
                )
            )
        } catch (e: Exception) {
            Result.failure(e.toAuthError())
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("User is null after sign in")
            Result.success(
                AuthResult(
                    uid = user.uid,
                    email = user.email,
                    isNewUser = result.additionalUserInfo?.isNewUser ?: false
                )
            )
        } catch (e: Exception) {
            Result.failure(e.toAuthError())
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.toAuthError())
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.toAuthError())
        }
    }

    override suspend fun deleteCurrentUser(): Result<Unit> {
        return try {
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e.toAuthError())
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
