package com.routecj.customer.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.customer.domain.repository.AuthRepository
import com.routecj.customer.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            verifyCustomerRole(userId)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    fun checkAuthStatus() {
        val userId = authRepository.getCurrentUserId()
        if (userId != null) {
            verifyCustomerRole(userId)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    private fun verifyCustomerRole(userId: String) {
        viewModelScope.launch {
            val result = customerRepository.getCustomer(userId)
            result.onSuccess { customer ->
                if (customer.role.trim().lowercase() == "customer" && customer.isActiveAccount) {
                    _authState.value = AuthState.Authenticated
                } else if (!customer.isActiveAccount) {
                    authRepository.signOut()
                    _authState.value = AuthState.Unauthorized("Your account is inactive. Please contact support.")
                } else {
                    authRepository.signOut()
                    _authState.value = AuthState.Unauthorized("Access denied. This account is not authorized for the Customer App.")
                }
            }.onFailure {
                // Do NOT sign out aggressively here. 
                // A new registration or Google Sign-In might be in the middle of creating the profile.
                _authState.value = AuthState.Unauthorized("Customer profile not found.")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Unauthenticated
        }
    }
}
