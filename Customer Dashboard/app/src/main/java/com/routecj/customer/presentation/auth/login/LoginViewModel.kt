package com.routecj.customer.presentation.auth.login

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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        _errorMessage.value = null
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        _errorMessage.value = null
    }

    fun loginWithEmail() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _errorMessage.value = "Email and Password cannot be empty."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = authRepository.signInWithEmail(_email.value, _password.value)
            result.onFailure { error ->
                _errorMessage.value = error.message
                _isLoading.value = false
            }.onSuccess { authResult ->
                val profileResult = customerRepository.getCustomer(authResult.uid)
                if (profileResult.isSuccess) {
                    val profile = profileResult.getOrThrow()
                    if (profile.role.trim().lowercase() != "customer") {
                        authRepository.signOut()
                        _errorMessage.value = "Access denied. This account is not authorized for the Customer App."
                    } else if (!profile.isActiveAccount) {
                        authRepository.signOut()
                        _errorMessage.value = "Your account is inactive. Please contact support."
                    } else {
                        _isSuccess.value = true
                    }
                } else {
                    authRepository.signOut()
                    _errorMessage.value = "Customer profile not found."
                }
                _isLoading.value = false
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _isLoading.value = true
        _errorMessage.value = null
        timber.log.Timber.tag("AUTH_GOOGLE").d("Firebase authentication started with ID token")

        viewModelScope.launch {
            val authResult = authRepository.signInWithGoogle(idToken)
            authResult.onSuccess { result ->
                timber.log.Timber.tag("AUTH_GOOGLE").d("Firebase authentication succeeded: uid=${result.uid}")
                // Check if customer profile exists
                val profileResult = customerRepository.getCustomer(result.uid)
                if (profileResult.isSuccess) {
                    val profile = profileResult.getOrThrow()
                    timber.log.Timber.tag("AUTH_GOOGLE").d("Customer profile found: role=${profile.role}")
                    if (profile.role.trim().lowercase() != "customer" || !profile.isActiveAccount) {
                        authRepository.signOut()
                        _errorMessage.value = "Access denied: Account is not an active Customer account."
                        _isLoading.value = false
                        timber.log.Timber.tag("AUTH_GOOGLE").w("Role or active validation failed: ${profile.role}, isActive=${profile.isActiveAccount}")
                        return@launch
                    }
                } else {
                    timber.log.Timber.tag("AUTH_GOOGLE").d("Customer profile not found. Creating new profile...")
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    if (firebaseUser == null || firebaseUser.uid != result.uid) {
                        timber.log.Timber.tag("AUTH_GOOGLE").e("Auth state not synchronized. currentUser is null or UID mismatch.")
                        _errorMessage.value = "Unable to create your customer profile. Please try again."
                        _isLoading.value = false
                        return@launch
                    }

                    val newCustomer = com.routecj.customer.domain.model.Customer(
                        id = result.uid,
                        email = result.email ?: firebaseUser.email ?: "",
                        name = firebaseUser.displayName ?: "Google User",
                        phoneNumber = "",
                        role = "customer",
                        isActiveAccount = true,
                        profileImageUrl = firebaseUser.photoUrl?.toString()
                    )
                    val createResult = customerRepository.createCustomer(newCustomer)
                    if (createResult.isFailure) {
                        timber.log.Timber.tag("AUTH_GOOGLE").e("Failed to create customer profile: ${createResult.exceptionOrNull()?.message}")
                        _errorMessage.value = "Failed to create customer profile. Please try again."
                        _isLoading.value = false
                        return@launch
                    }
                    timber.log.Timber.tag("AUTH_GOOGLE").d("Customer profile created successfully")
                }
                _isLoading.value = false
                _isSuccess.value = true
            }.onFailure { error ->
                timber.log.Timber.tag("AUTH_GOOGLE").e("Firebase authentication failed: ${error.message}")
                _errorMessage.value = "Unable to sign in with Google. Please try again."
                _isLoading.value = false
            }
        }
    }
}
