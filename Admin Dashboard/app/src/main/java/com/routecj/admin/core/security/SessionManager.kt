package com.routecj.admin.core.security

import com.routecj.admin.domain.model.Admin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton class to manage the current active session and admin profile.
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _currentAdmin = MutableStateFlow<Admin?>(null)
    val currentAdmin: StateFlow<Admin?> = _currentAdmin.asStateFlow()

    /**
     * Updates the current admin profile in the session.
     */
    fun updateAdmin(admin: Admin?) {
        _currentAdmin.value = admin
    }

    /**
     * Clears the current session data.
     */
    fun clearSession() {
        _currentAdmin.value = null
    }

    /**
     * Helper to get the current role quickly.
     */
    val currentRole get() = _currentAdmin.value?.role
}
