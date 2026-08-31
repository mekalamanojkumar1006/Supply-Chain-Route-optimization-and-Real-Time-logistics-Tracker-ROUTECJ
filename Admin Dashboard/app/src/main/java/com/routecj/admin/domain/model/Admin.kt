package com.routecj.admin.domain.model

/**
 * Domain model representing an Administrator/User in the system.
 */
data class Admin(
    val uid: String = "",
    val adminId: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: AdminRole = AdminRole.UNKNOWN,
    val status: String = "",
    val lastLogin: String = "",
    val profileImage: String? = null,
    
    // Notification Preferences
    val notificationsEnabled: Boolean = true,
    val orderAlertsEnabled: Boolean = true,
    val dispatchAlertsEnabled: Boolean = true,
    val driverAlertsEnabled: Boolean = true
) {
    /**
     * Helper properties to check user roles easily.
     */
    val isSuperAdmin: Boolean get() = role == AdminRole.SUPER_ADMIN
    val isAdmin: Boolean get() = role == AdminRole.ADMIN || role == AdminRole.SUPER_ADMIN
    val isDispatchManager: Boolean get() = role == AdminRole.DISPATCH_MANAGER
    val isGodownManager: Boolean get() = role == AdminRole.GODOWN_MANAGER
    val isActiveAccount: Boolean get() = status.isBlank() || status.equals("ACTIVE", ignoreCase = true)
}
