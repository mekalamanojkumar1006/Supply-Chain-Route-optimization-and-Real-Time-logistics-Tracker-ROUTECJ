package com.routecj.admin.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.core.util.Result
import com.routecj.admin.data.model.AdminDto
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.repository.ProfileRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ProfileRepository using Firebase.
 * Fetches the current logged-in admin's profile from Firestore.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ProfileRepository {

    override suspend fun getCurrentAdminProfile(): Result<Admin> {
        return try {
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                Log.e("PROFILE", "No authenticated user found")
                return Result.Error("User not authenticated")
            }
            fetchProfile(uid)
        } catch (e: Exception) {
            Log.e("PROFILE", "Error fetching profile", e)
            Result.Error("Database error: ${e.message}")
        }
    }

    override suspend fun updateAdminProfile(admin: Admin): Result<Unit> {
        return try {
            val uid = firebaseAuth.currentUser?.uid ?: return Result.Error("User not authenticated")
            
            val updates = mutableMapOf<String, Any?>(
                "name" to admin.name,
                "phone" to admin.phone,
                "profileImage" to (admin.profileImage ?: ""),
                "notificationsEnabled" to admin.notificationsEnabled,
                "orderAlertsEnabled" to admin.orderAlertsEnabled,
                "dispatchAlertsEnabled" to admin.dispatchAlertsEnabled,
                "driverAlertsEnabled" to admin.driverAlertsEnabled
            )

            firestore.collection("admins").document(uid).update(updates).await()
            
            // Also update Firebase Auth profile
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                displayName = admin.name
                if (admin.profileImage != null) {
                    photoUri = android.net.Uri.parse(admin.profileImage)
                }
            }
            firebaseAuth.currentUser?.updateProfile(profileUpdates)?.await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e("PROFILE", "Error updating profile", e)
            Result.Error("Failed to update profile: ${e.message}")
        }
    }

    private suspend fun fetchProfile(uid: String): Result<Admin> {
        val currentUser = firebaseAuth.currentUser
        val email = currentUser?.email
        val collectionRef = firestore.collection("admins")

        Log.d("PROFILE", "Starting profile fetch for UID: $uid")

        // STEP 1: CANONICAL UID LOOKUP
        try {
            val uidDoc = collectionRef.document(uid).get().await()
            if (uidDoc.exists()) {
                Log.d("PROFILE", "Canonical profile found at admins/$uid")
                return parseAdmin(uidDoc.toObject(AdminDto::class.java))
            }
        } catch (e: Exception) {
            Log.e("PROFILE", "Canonical lookup failed: ${e.message}")
        }

        // STEP 2: DISCOVERY SEARCH
        var legacyDoc: com.google.firebase.firestore.DocumentSnapshot? = null

        // 2.1 Search by Email
        if (!email.isNullOrBlank()) {
            val emailOptions = listOf(email.trim(), email.trim().lowercase())
            for (emailToTry in emailOptions.distinct()) {
                try {
                    val emailQuery = collectionRef.whereEqualTo("email", emailToTry).limit(1).get().await()
                    if (!emailQuery.isEmpty) {
                        legacyDoc = emailQuery.documents[0]
                        Log.i("PROFILE", "Found legacy profile via email search: ${legacyDoc.id}")
                        break
                    }
                } catch (e: Exception) {
                    Log.w("PROFILE", "Email search ('$emailToTry') failed: ${e.message}")
                }
            }
        }

        // 2.2 Legacy ID Fallback
        if (legacyDoc == null) {
            val legacyIds = listOf("ADMIN001", "ADMIN002", "ADMIN003", "ADMIN004")
            for (legacyId in legacyIds) {
                try {
                    val doc = collectionRef.document(legacyId).get().await()
                    if (doc.exists()) {
                        val docEmail = doc.getString("email")
                        if (docEmail?.trim()?.lowercase() == email?.trim()?.lowercase()) {
                            legacyDoc = doc
                            Log.i("PROFILE", "Found legacy profile via document ID: $legacyId")
                            break
                        }
                    }
                } catch (e: Exception) {
                    // SILENT
                }
            }
        }

        // STEP 3: PERFORM ONE-TIME MIGRATION
        if (legacyDoc != null) {
            Log.i("PROFILE", "Migrating legacy profile ${legacyDoc.id} to UID-keyed document $uid")
            try {
                val data = legacyDoc.data?.toMutableMap() ?: mutableMapOf()
                data["uid"] = uid
                data["email"] = email ?: data["email"] ?: ""
                if (!data.containsKey("adminId")) {
                    data["adminId"] = legacyDoc.id
                }

                collectionRef.document(uid).set(data).await()
                Log.i("PROFILE", "Migration successful for $uid")
                
                val newDoc = collectionRef.document(uid).get().await()
                return parseAdmin(newDoc.toObject(AdminDto::class.java))
            } catch (e: Exception) {
                Log.e("PROFILE", "One-time migration failed", e)
                return parseAdmin(legacyDoc.toObject(AdminDto::class.java))
            }
        }

        Log.e("PROFILE", "No profile found for UID $uid and Email $email")
        return Result.Error("Profile not found")
    }

    private fun parseAdmin(adminDto: AdminDto?): Result<Admin> {
        return if (adminDto != null) {
            var admin = adminDto.toDomain()
            if (admin.lastLogin.isBlank()) {
                val lastSignIn = firebaseAuth.currentUser?.metadata?.lastSignInTimestamp
                val formattedDate = if (lastSignIn != null && lastSignIn > 0) {
                    java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                        .format(java.util.Date(lastSignIn))
                } else {
                    "N/A"
                }
                admin = admin.copy(lastLogin = formattedDate)
            }
            if (admin.phone.isBlank()) {
                admin = admin.copy(phone = "Not Available")
            }
            Log.d("PROFILE", "Profile loaded: Name=${admin.name}, Role=${admin.role}")
            Result.Success(admin)
        } else {
            Log.e("PROFILE", "Failed to parse admin data")
            Result.Error("Failed to parse admin profile data")
        }
    }
}
