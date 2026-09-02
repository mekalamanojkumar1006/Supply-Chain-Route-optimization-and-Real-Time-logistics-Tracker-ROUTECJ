package com.routecj.driver.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.driver.core.util.Result
import com.routecj.driver.data.mapper.DriverMapper
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Implementation of AuthRepository connecting FirebaseAuth and Firestore 'drivers' collection.
 */
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    override fun getCurrentFirebaseUser(): FirebaseUser? = firebaseAuth.currentUser

    override fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override suspend fun login(email: String, password: String): Result<Driver> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = authResult.user ?: return Result.Error("Authentication failed: No user record")
            
            // Resolve Driver Profile from Firestore
            resolveDriverProfile(user.uid, user.email)
        } catch (e: FirebaseAuthException) {
            val friendlyMessage = when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
                "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL" -> "Invalid email or password."
                "ERROR_USER_DISABLED" -> "This driver account has been disabled."
                "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts. Please try again later."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your internet connection."
                else -> e.message ?: "Authentication error"
            }
            Result.Error(friendlyMessage, e)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Login failed. Please try again.", e)
        }
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            Result.Success(Unit)
        } catch (e: FirebaseAuthException) {
            val friendlyMessage = when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> "Please enter a valid email address."
                "ERROR_USER_NOT_FOUND" -> "No user found with this email."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection."
                else -> e.message ?: "Failed to send password reset email."
            }
            Result.Error(friendlyMessage, e)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to send reset email.", e)
        }
    }

    override suspend fun getCurrentDriver(): Result<Driver> {
        val user = firebaseAuth.currentUser ?: return Result.Error("User is not authenticated")
        return resolveDriverProfile(user.uid, user.email)
    }

    override fun logout() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    firestore.collection("drivers").document(currentUser.uid).update(
                        mapOf(
                            "fcmToken" to "",
                            "notificationToken" to ""
                        )
                    ).await()
                } catch (e: Exception) {
                    // Ignore errors during logout cleanup
                }
                firebaseAuth.signOut()
            }
        } else {
            firebaseAuth.signOut()
        }
    }

    /**
     * Resolves the Driver profile in Firestore.
     * Strategy:
     * 1. Direct lookup by UID: drivers/{uid}
     * 2. Query lookup by email: drivers.whereEqualTo("email", email)
     */
    private suspend fun resolveDriverProfile(uid: String, email: String?): Result<Driver> {
        val collectionRef = firestore.collection("drivers")

        // 1. Direct UID Document Lookup
        try {
            val uidDoc = collectionRef.document(uid).get().await()
            if (uidDoc.exists()) {
                val driver = DriverMapper.mapToDomain(uidDoc.id, uidDoc.data)
                return Result.Success(driver)
            }
        } catch (e: Exception) {
            // Fallthrough to email lookup if UID doc fails or doesn't exist
        }

        // 2. Email Query Lookup
        if (!email.isNullOrBlank()) {
            try {
                val emailQuery = collectionRef.whereEqualTo("email", email.trim().lowercase()).limit(1).get().await()
                if (!emailQuery.isEmpty) {
                    val doc = emailQuery.documents[0]
                    val driver = DriverMapper.mapToDomain(doc.id, doc.data)
                    return Result.Success(driver)
                }
            } catch (e: Exception) {
                return Result.Error("Error verifying driver account: ${e.message}", e)
            }
        }

        // Driver not found in authorized drivers collection
        return Result.Error("DRIVER_ACCOUNT_NOT_FOUND: Your account is authenticated, but no authorized Driver profile was found.")
    }
}
