package com.routecj.admin.core.util

import com.routecj.admin.domain.model.AdminRole
import java.util.Locale

/**
 * RouteCJ Email Generation & Normalization Utilities.
 * Enforces company email formats:
 * - Driver: <drivername>@routecj.com (e.g. Ramesh Kumar -> ramesh.kumar@routecj.com)
 * - Admin roles: admin@routecj.com, godown.manager@routecj.com, dispatch.manager@routecj.com
 */
object EmailUtils {

    const val ROUTECJ_DOMAIN = "routecj.com"

    /**
     * Generates a normalized company email for a driver.
     * Rules:
     * - Lowercase
     * - Trimmed leading/trailing spaces
     * - Replaces spaces and special characters with dot separators
     * - Eliminates consecutive dots and trailing dots before the @ domain
     */
    fun generateDriverEmail(name: String): String {
        val sanitized = sanitizeNameForEmail(name)
        val prefix = if (sanitized.isNotBlank()) sanitized else "driver"
        return "$prefix@$ROUTECJ_DOMAIN"
    }

    /**
     * Generates a default company email for administrative accounts.
     */
    fun generateAdminEmail(role: AdminRole, name: String? = null): String {
        val customNamePart = if (!name.isNullOrBlank()) sanitizeNameForEmail(name) else null

        val prefix = when (role) {
            AdminRole.SUPER_ADMIN -> customNamePart ?: "super.admin"
            AdminRole.ADMIN -> customNamePart ?: "admin"
            AdminRole.GODOWN_MANAGER -> customNamePart?.let { "godown.$it" } ?: "godown.manager"
            AdminRole.DISPATCH_MANAGER -> customNamePart?.let { "dispatch.$it" } ?: "dispatch.manager"
            AdminRole.UNKNOWN -> customNamePart ?: "user"
        }
        return "$prefix@$ROUTECJ_DOMAIN"
    }

    /**
     * Sanitizes a person's name to create a valid email local-part.
     */
    fun sanitizeNameForEmail(name: String): String {
        return name
            .trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), ".") // Replace non-alphanumeric with dots
            .replace(Regex("\\.+"), ".")        // Collapse multiple dots into one
            .trim('.')                          // Remove leading and trailing dots
    }

    /**
     * Verifies if an email belongs to the RouteCJ domain.
     */
    fun isRouteCjEmail(email: String): Boolean {
        return email.trim().lowercase(Locale.ROOT).endsWith("@$ROUTECJ_DOMAIN")
    }
}
