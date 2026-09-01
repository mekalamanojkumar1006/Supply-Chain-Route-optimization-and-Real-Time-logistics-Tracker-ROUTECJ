package com.routecj.admin.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Splash Screen.
 * Handles app initialization and session checks.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        performInitialization()
    }

    private fun performInitialization() {
        viewModelScope.launch {
            Timber.tag("AUTH_STARTUP").d("Starting initialization...")
            val startTime = System.currentTimeMillis()
            
            // 1. Check if Firebase Auth session exists
            val isLoggedIn = authRepository.isUserLoggedIn()
            Timber.tag("AUTH_STARTUP").d("Initial Firebase auth status: isLoggedIn=$isLoggedIn")
            
            var finalDestination = com.routecj.admin.core.util.Constants.NavigationRoutes.LOGIN
            
            if (isLoggedIn) {
                // 2. Fetch/Validate Admin Profile from Firestore
                Timber.tag("AUTH_STARTUP").d("Fetching admin profile for session verification...")
                when (val result = authRepository.getCurrentAdmin()) {
                    is Result.Success -> {
                        val admin = result.data
                        if (admin != null && admin.role != AdminRole.UNKNOWN) {
                            if (!admin.isActiveAccount) {
                                Timber.tag("AUTH_STARTUP").w("Admin account is inactive/suspended. Redirecting to login.")
                                authRepository.logout()
                                finalDestination = com.routecj.admin.core.util.Constants.NavigationRoutes.LOGIN
                            } else {
                                Timber.tag("AUTH_STARTUP").d("Valid Admin session found. UID: ${admin.uid}, Role: ${admin.role}")
                                finalDestination = com.routecj.admin.core.util.Constants.NavigationRoutes.DASHBOARD
                            }
                        } else {
                            Timber.tag("AUTH_STARTUP").w("Session valid but Admin profile invalid or missing role. Redirecting to login.")
                            authRepository.logout()
                        }
                    }
                    is Result.Error -> {
                        Timber.tag("AUTH_STARTUP").e("Profile verification failed: ${result.message}. Logging out.")
                        authRepository.logout()
                    }
                    else -> {
                        Timber.tag("AUTH_STARTUP").d("Profile loading or unexpected state. Defaulting to login.")
                        authRepository.logout()
                    }
                }
            } else {
                Timber.tag("AUTH_STARTUP").d("No existing Firebase session found.")
            }
            
            // Calculate remaining time for animation (Target total: 3.2s)
            val elapsedTime = System.currentTimeMillis() - startTime
            val minDuration = 3200L
            if (elapsedTime < minDuration) {
                delay(minDuration - elapsedTime)
            }
            
            Timber.tag("AUTH_STARTUP").d("Final navigation decision: $finalDestination")
            _startDestination.value = finalDestination
            _isInitialized.value = true
        }
    }
}
