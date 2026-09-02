package com.routecj.admin.core.security

import com.routecj.admin.domain.model.AdminRole

/**
 * Centralized Permission Manager for the application.
 * Defines access control logic for all features based on Admin roles.
 */
object PermissionManager {

    enum class AppFeature {
        DASHBOARD,
        ORDERS,
        DRIVERS,
        VEHICLES,
        DISPATCH,
        GODOWNS,
        TRACKING,
        REPORTS,
        NOTIFICATIONS,
        PROFILE,
        SETTINGS,
        USER_MANAGEMENT,
        INVENTORY
    }

    /**
     * Checks if a specific role has permission to access a feature.
     */
    fun hasPermission(role: AdminRole, feature: AppFeature): Boolean {
        return when (role) {
            AdminRole.SUPER_ADMIN -> true // Full access
            
            AdminRole.ADMIN -> when (feature) {
                AppFeature.USER_MANAGEMENT -> false
                AppFeature.SETTINGS -> false // Not explicitly mentioned, but likely restricted
                else -> true
            }
            
            AdminRole.GODOWN_MANAGER -> when (feature) {
                AppFeature.DASHBOARD,
                AppFeature.GODOWNS,
                AppFeature.ORDERS,
                AppFeature.INVENTORY,
                AppFeature.REPORTS,
                AppFeature.NOTIFICATIONS,
                AppFeature.PROFILE -> true
                else -> false
            }
            
            AdminRole.DISPATCH_MANAGER -> when (feature) {
                AppFeature.DASHBOARD,
                AppFeature.ORDERS,
                AppFeature.DISPATCH,
                AppFeature.TRACKING,
                AppFeature.DRIVERS,
                AppFeature.VEHICLES,
                AppFeature.NOTIFICATIONS,
                AppFeature.PROFILE -> true
                else -> false
            }
            
            AdminRole.UNKNOWN -> false
        }
    }

    /**
     * Checks if a role has "View Only" permission for a feature.
     * Some roles can view but not manage certain modules.
     */
    fun canManage(role: AdminRole, feature: AppFeature): Boolean {
        return when (role) {
            AdminRole.SUPER_ADMIN -> true
            AdminRole.ADMIN -> !listOf(AppFeature.USER_MANAGEMENT, AppFeature.SETTINGS).contains(feature)
            
            AdminRole.GODOWN_MANAGER -> when (feature) {
                AppFeature.GODOWNS, AppFeature.INVENTORY -> true
                else -> false // Can view Orders/Reports but not manage? User said "Orders (View)", "Reports (View)"
            }
            
            AdminRole.DISPATCH_MANAGER -> when (feature) {
                AppFeature.DISPATCH, AppFeature.TRACKING -> true
                else -> false // Can view Drivers/Vehicles but not manage? User said "Drivers (View)", "Vehicles (View)"
            }
            
            AdminRole.UNKNOWN -> false
        }
    }
}
