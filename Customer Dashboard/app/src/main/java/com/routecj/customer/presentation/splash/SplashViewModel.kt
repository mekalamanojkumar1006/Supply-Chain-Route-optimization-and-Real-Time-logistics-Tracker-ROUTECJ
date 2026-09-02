package com.routecj.customer.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.customer.domain.repository.AuthRepository
import com.routecj.customer.domain.repository.CustomerRepository
import com.routecj.customer.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _isInitialized = MutableStateFlow(value = false)
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

            val userId = authRepository.getCurrentUserId()
            var finalDestination = Screen.Login.route

            if (userId != null) {
                Timber.tag("AUTH_STARTUP").d("Fetching customer profile...")
                val result = customerRepository.getCustomer(userId)
                result.onSuccess { customer ->
                    if ((customer.role.trim().lowercase() == "customer") && (customer.isActiveAccount)) {
                        Timber.tag("AUTH_STARTUP").d("Valid Customer session found.")
                        finalDestination = Screen.Home.route
                    } else {
                        Timber.tag("AUTH_STARTUP").w("Role or active account validation failed.")
                        authRepository.signOut()
                    }
                }.onFailure { error ->
                    Timber.tag("AUTH_STARTUP").e("Failed to fetch customer profile: ${error.message}")
                    authRepository.signOut()
                }
            } else {
                Timber.tag("AUTH_STARTUP").d("No active Firebase session found.")
            }

            val elapsedTime = System.currentTimeMillis() - startTime
            val minDuration = 3200L
            if (elapsedTime < minDuration) {
                delay((minDuration - elapsedTime).milliseconds)
            }

            _startDestination.value = finalDestination
            _isInitialized.value = true
        }
    }
}
