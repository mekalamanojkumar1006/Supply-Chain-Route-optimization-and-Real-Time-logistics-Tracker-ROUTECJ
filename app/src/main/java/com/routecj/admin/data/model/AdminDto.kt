package com.routecj.admin.data.model

import android.util.Log
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.model.AdminRole

/**
 * Data Transfer Object for Admin documents in Firestore.
 * Used for deserialization from Firebase.
 */
data class AdminDto(
    val uid: String = "",
    val adminId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val status: String = "",
    val lastLogin: Any? = null,
    val profileImage: String? = null,
    val notificationsEnabled: Boolean = true,
    val orderAlertsEnabled: Boolean = true,
    val dispatchAlertsEnabled: Boolean = true,
    val driverAlertsEnabled: Boolean = true
) {
    /**
     * Maps the DTO to the Domain model.
     */
    fun toDomain(): Admin {
        val matchedRole = AdminRole.fromId(role)
        
        Log.d("ROLE_DEBUG", "Firestore Role: '$role'")
        Log.d("ROLE_DEBUG", "Normalized Role: '${role.trim().lowercase()}'")
        Log.d("ROLE_DEBUG", "Matched Role Enum: $matchedRole")
        
        val formattedLastLogin = when (lastLogin) {
            is com.google.firebase.Timestamp -> {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(lastLogin.toDate())
            }
            is java.util.Date -> {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(lastLogin)
            }
            is Number -> {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastLogin.toLong()))
            }
            is String -> lastLogin
            else -> ""
        }

        return Admin(
            uid = uid,
            adminId = adminId,
            name = name,
            email = email,
            phone = phone,
            role = matchedRole,
            status = status,
            lastLogin = formattedLastLogin,
            profileImage = profileImage,
            notificationsEnabled = notificationsEnabled,
            orderAlertsEnabled = orderAlertsEnabled,
            dispatchAlertsEnabled = dispatchAlertsEnabled,
            driverAlertsEnabled = driverAlertsEnabled
        )
    }
}
