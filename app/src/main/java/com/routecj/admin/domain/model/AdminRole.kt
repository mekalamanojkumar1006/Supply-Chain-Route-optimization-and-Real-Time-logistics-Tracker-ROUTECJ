package com.routecj.admin.domain.model

/**
 * Enum representing the different roles available in the system.
 */
enum class AdminRole(val roleId: String, val displayName: String) {
    SUPER_ADMIN("ADMIN001", "Super Admin"),
    ADMIN("ADMIN002", "Admin"),
    GODOWN_MANAGER("ADMIN003", "Godown Manager"),
    DISPATCH_MANAGER("ADMIN004", "Dispatch Manager"),
    UNKNOWN("UNKNOWN", "Unknown");

    companion object {
        fun fromId(id: String?): AdminRole {
            if (id == null) return UNKNOWN
            
            val normalized = id.trim().lowercase().replace(" ", "_")
            
            // 1. Match by roleId (ADMIN001, etc.)
            val matchById = entries.find { it.roleId.lowercase() == normalized }
            if (matchById != null) return matchById
            
            // 2. Match by enum name (SUPER_ADMIN, etc.)
            val matchByName = entries.find { it.name.lowercase() == normalized }
            if (matchByName != null) return matchByName
            
            // 3. Match by display name normalized
            val matchByDisplay = entries.find { 
                it.displayName.trim().lowercase().replace(" ", "_") == normalized 
            }
            if (matchByDisplay != null) return matchByDisplay
            
            // 4. Special cases for common legacy strings
            return when (normalized) {
                "super_admin" -> SUPER_ADMIN
                "admin" -> ADMIN
                "godown_manager" -> GODOWN_MANAGER
                "dispatch_manager" -> DISPATCH_MANAGER
                else -> UNKNOWN
            }
        }
    }
}
