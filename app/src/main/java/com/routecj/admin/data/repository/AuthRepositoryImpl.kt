package com.routecj.admin.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.routecj.admin.core.security.SessionManager
import com.routecj.admin.core.util.Result
import com.routecj.admin.data.model.AdminDto
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using Firebase.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) : AuthRepository {

    private val TAG = "AuthRepositoryImpl"

    override suspend fun login(email: String, password: String): Result<Admin> {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        return try {
            Timber.tag("AUTH_DEBUG").d("CALLING signInWithEmailAndPassword for email: $trimmedEmail")
            Timber.tag("AUTH_FIRESTORE").d("1. Firebase Auth sign-in started for email: $trimmedEmail")
            // 1. Authenticate with Firebase Auth
            val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
            val firebaseUser = authResult.user ?: return Result.Error("Authentication failed: No UID returned")
            Timber.tag("AUTH_DEBUG").d("LOGIN SUCCESS. UID: ${firebaseUser.uid}")
            Timber.tag("AUTH_FIRESTORE").d("2. Firebase Auth success. UID: ${firebaseUser.uid}")
            Timber.tag("AUTH_FIRESTORE").d("3. Authenticated email: ${firebaseUser.email}")
            Timber.tag("AUTH_FIRESTORE").d("AUTH_SUCCESS")

            // 2. Fetch Admin details from Firestore
            val result = fetchAdminFromFirestore(firebaseUser.uid)
            if (result is Result.Success) {
                val admin = result.data
                if (admin.status.isNotBlank() && (admin.status.equals("INACTIVE", ignoreCase = true) || admin.status.equals("SUSPENDED", ignoreCase = true))) {
                    Timber.tag("AUTH_FIRESTORE").w("Admin account is inactive/suspended: ${admin.status}")
                    logout()
                    return Result.Error("Your RouteCJ account is currently inactive. Contact an administrator.")
                }

                // 3. Update Last Login Server Timestamp in Firestore (Non-blocking / Non-fatal)
                try {
                    val timestampUpdates = mapOf<String, Any>(
                        "lastLogin" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    firestore.collection("admins").document(firebaseUser.uid).update(timestampUpdates).await()
                    if (admin.adminId.isNotBlank() && admin.adminId != firebaseUser.uid) {
                        try {
                            firestore.collection("admins").document(admin.adminId).update(timestampUpdates).await()
                        } catch (_: Exception) {}
                    }
                    Timber.tag("AUTH_FIRESTORE").d("Updated lastLogin server timestamp for ${admin.adminId} (${firebaseUser.uid})")
                } catch (e: Exception) {
                    Timber.tag("AUTH_FIRESTORE").w(e, "Non-fatal: Failed to update lastLogin timestamp in Firestore")
                }

                sessionManager.updateAdmin(admin)
            }
            result
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Timber.tag("AUTH_FIRESTORE").e(e, "Firebase Auth failure: Invalid credentials for $trimmedEmail")
            Result.Error("Invalid email or password. Please check your credentials and try again.")
        } catch (e: Exception) {
            Timber.tag("AUTH_FIRESTORE").e(e, "Firebase Auth failure")
            Result.Error(e.message ?: "An unexpected error occurred during login")
        }
    }

    override suspend fun getCurrentAdmin(): Result<Admin?> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                sessionManager.clearSession()
                Result.Success(null)
            } else {
                when (val result = fetchAdminFromFirestore(uid)) {
                    is Result.Success -> {
                        sessionManager.updateAdmin(result.data)
                        Result.Success(result.data)
                    }
                    is Result.Error -> Result.Error(result.message, result.code, result.throwable)
                    is Result.Loading -> Result.Loading()
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Failed to fetch current user session")
        }
    }

    override fun observeAdminProfile(uid: String): Flow<Result<Admin>> = callbackFlow {
        var querySubscription: ListenerRegistration? = null
        
        val docSubscription = firestore.collection("admins").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.Error(error.message ?: "Unknown error"))
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    querySubscription?.remove() // If we found it by ID, stop searching by field
                    val adminDto = snapshot.toObject(AdminDto::class.java)
                    if (adminDto != null) {
                        val admin = adminDto.toDomain()
                        sessionManager.updateAdmin(admin)
                        trySend(Result.Success(admin))
                    }
                } else if (querySubscription == null) {
                    // Try searching by uid field if document ID is different
                    querySubscription = firestore.collection("admins").whereEqualTo("uid", uid)
                        .addSnapshotListener { querySnapshot, queryError ->
                            if (queryError != null) {
                                trySend(Result.Error(queryError.message ?: "Unknown error"))
                                return@addSnapshotListener
                            }
                            if (querySnapshot != null && !querySnapshot.isEmpty) {
                                val doc = querySnapshot.documents[0]
                                val adminDto = doc.toObject(AdminDto::class.java)
                                if (adminDto != null) {
                                    val admin = adminDto.toDomain()
                                    sessionManager.updateAdmin(admin)
                                    trySend(Result.Success(admin))
                                }
                            }
                        }
                }
            }
        
        awaitClose { 
            docSubscription.remove()
            querySubscription?.remove()
        }
    }

    override suspend fun logout() {
        firebaseAuth.signOut()
        sessionManager.clearSession()
        Timber.tag("AUTH_DEBUG").d("AFTER SIGNOUT currentUser = ${firebaseAuth.currentUser != null}")
    }

    override fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            Log.d(TAG, "Attempting to send password reset email to: $email")
            firebaseAuth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent successfully to: $email")
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send password reset email to: $email", e)
            Result.Error(e.message ?: "Failed to send reset email")
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser ?: return Result.Error("No user logged in")
            val email = user.email ?: return Result.Error("User email not found")
            
            // Re-authenticate
            val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            
            // Update password
            user.updatePassword(newPassword).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password change failed", e)
            Result.Error(e.message ?: "Failed to change password")
        }
    }

    /**
     * Helper function to fetch admin data from Firestore "admins" collection.
     * Uses a multi-stage lookup with automatic migration to UID-keyed documents.
     */
    private suspend fun fetchAdminFromFirestore(uid: String): Result<Admin> {
        val currentUser = firebaseAuth.currentUser
        val email = currentUser?.email
        val collectionRef = firestore.collection("admins")

        Timber.tag("AUTH_FIRESTORE").d("Starting admin profile fetch for UID: $uid")

        // STEP 1: CANONICAL UID LOOKUP (Optimized for migrated users)
        try {
            val uidDoc = collectionRef.document(uid).get().await()
            if (uidDoc.exists()) {
                Timber.tag("AUTH_FIRESTORE").d("Canonical profile found at admins/$uid")
                return parseDocument(uidDoc)
            }
        } catch (e: Exception) {
            val code = (e as? FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"
            Timber.tag("AUTH_FIRESTORE").e("Canonical lookup failed: $code - ${e.message}")
            if (code == "PERMISSION_DENIED") {
                return Result.Error("Security Error: Access denied for profile admins/$uid. Contact Super Admin.")
            }
        }

        // STEP 2: DISCOVERY SEARCH (For unmigrated/legacy users)
        var legacyDoc: com.google.firebase.firestore.DocumentSnapshot? = null

        // 2.1 Search by Email (Tries both exact and lowercase)
        if (!email.isNullOrBlank()) {
            val emailOptions = listOf(email.trim(), email.trim().lowercase())
            for (emailToTry in emailOptions.distinct()) {
                try {
                    val emailQuery = collectionRef.whereEqualTo("email", emailToTry).limit(1).get().await()
                    if (!emailQuery.isEmpty) {
                        legacyDoc = emailQuery.documents[0]
                        Timber.tag("AUTH_FIRESTORE").i("Found legacy profile via email search: ${legacyDoc.id}")
                        break
                    }
                } catch (e: Exception) {
                    val code = (e as? FirebaseFirestoreException)?.code?.name ?: "UNKNOWN"
                    Timber.tag("AUTH_FIRESTORE").w("Email search ('$emailToTry') failed: $code")
                    if (code == "PERMISSION_DENIED") {
                        return Result.Error("Security Error: Access denied during email lookup. Contact Super Admin.")
                    }
                }
            }
        }

        // 2.2 Legacy ID Fallback (ADMIN001, etc.)
        if (legacyDoc == null) {
            val legacyIds = listOf("ADMIN001", "ADMIN002", "ADMIN003", "ADMIN004")
            for (legacyId in legacyIds) {
                try {
                    val doc = collectionRef.document(legacyId).get().await()
                    if (doc.exists()) {
                        val docEmail = doc.getString("email")
                        if (docEmail?.trim()?.lowercase() == email?.trim()?.lowercase()) {
                            legacyDoc = doc
                            Timber.tag("AUTH_FIRESTORE").i("Found legacy profile via document ID: $legacyId")
                            break
                        }
                    }
                } catch (e: Exception) {
                    // SILENT: Expected for most users as they won't have permission to check other legacy IDs
                }
            }
        }

        // STEP 3: PERFORM ONE-TIME MIGRATION
        if (legacyDoc != null) {
            Timber.tag("AUTH_FIRESTORE").i("Migrating legacy profile ${legacyDoc.id} to UID-keyed document $uid")
            try {
                val data = legacyDoc.data?.toMutableMap() ?: mutableMapOf()
                data["uid"] = uid
                data["email"] = email ?: data["email"] ?: ""
                if (!data.containsKey("adminId")) {
                    data["adminId"] = legacyDoc.id
                }

                // Write to canonical path: admins/{uid}
                collectionRef.document(uid).set(data).await()
                Timber.tag("AUTH_FIRESTORE").i("Migration successful for $uid")
                
                val newDoc = collectionRef.document(uid).get().await()
                return parseDocument(newDoc)
            } catch (e: Exception) {
                Timber.tag("AUTH_FIRESTORE").e(e, "One-time migration failed")
                // If migration fails, we still try to parse the legacy doc to allow login
                return parseDocument(legacyDoc)
            }
        }

        Timber.tag("AUTH_FIRESTORE").e("No admin profile found for UID $uid and Email $email")
        return Result.Error("Admin profile not registered. Please ensure your account has been provisioned by a Super Admin.")
    }

    private fun parseDocument(doc: com.google.firebase.firestore.DocumentSnapshot): Result<Admin> {
        Timber.tag("AUTH_FIRESTORE").d("parseDocument called for ID: ${doc.id}")
        val adminDto = doc.toObject(AdminDto::class.java)
        return if (adminDto != null) {
            try {
                val domainModel = adminDto.toDomain()
                Timber.tag("AUTH_FIRESTORE").d("toDomain() SUCCESS for ${domainModel.adminId}")
                Result.Success(domainModel)
            } catch (e: Exception) {
                Timber.tag("AUTH_FIRESTORE").e(e, "toDomain() FAILED")
                Result.Error("Data mapping error: ${e.message}")
            }
        } else {
            Result.Error("Failed to parse admin data")
        }
    }
}
