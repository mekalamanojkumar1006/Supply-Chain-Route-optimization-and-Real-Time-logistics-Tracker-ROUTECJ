package com.routecj.admin.presentation.login

import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.util.Result
import com.routecj.admin.data.local.UserPreferences
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.usecase.ForgotPasswordUseCase
import com.routecj.admin.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Login ViewModel.
 * Manages authentication state and operations.
 *
 * @HiltViewModel - Makes this ViewModel injectable with Hilt
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val forgotPasswordUseCase: ForgotPasswordUseCase,
    private val userPreferences: UserPreferences
) : BaseViewModel() {

    // Email field state
    private val _emailState = MutableStateFlow("")
    val emailState: StateFlow<String> = _emailState.asStateFlow()

    // Password field state
    private val _passwordState = MutableStateFlow("")
    val passwordState: StateFlow<String> = _passwordState.asStateFlow()

    // Remember Me state
    private val _rememberMeState = MutableStateFlow(false)
    val rememberMeState: StateFlow<Boolean> = _rememberMeState.asStateFlow()

    // Loading state
    private val _isLoadingState = MutableStateFlow(false)
    val isLoadingState: StateFlow<Boolean> = _isLoadingState.asStateFlow()

    // Error state
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    // Success message state (for password reset)
    private val _successMessageState = MutableStateFlow<String?>(null)
    val successMessageState: StateFlow<String?> = _successMessageState.asStateFlow()

    // Password reset success dialog state
    private val _showResetSuccessDialog = MutableStateFlow(false)
    val showResetSuccessDialog: StateFlow<Boolean> = _showResetSuccessDialog.asStateFlow()

    // Login success state
    private val _loginSuccessState = MutableStateFlow(false)
    val loginSuccessState: StateFlow<Boolean> = _loginSuccessState.asStateFlow()

    // Current authenticated admin
    private val _currentAdmin = MutableStateFlow<Admin?>(null)
    val currentAdmin: StateFlow<Admin?> = _currentAdmin.asStateFlow()

    init {
        loadSavedCredentials()
    }

    private fun loadSavedCredentials() {
        launchIO {
            val savedEmail = userPreferences.savedEmail.first()
            val rememberMe = userPreferences.rememberMe.first()
            withMain {
                if (rememberMe && savedEmail != null) {
                    _emailState.value = savedEmail
                    _rememberMeState.value = true
                }
            }
        }
    }

    /**
     * Update email field.
     */
    fun setEmail(email: String) {
        _emailState.value = email
        _errorState.value = null
        _successMessageState.value = null
    }

    /**
     * Update password field.
     */
    fun setPassword(password: String) {
        _passwordState.value = password
        _errorState.value = null
        _successMessageState.value = null
    }

    /**
     * Update remember me state.
     */
    fun setRememberMe(remember: Boolean) {
        _rememberMeState.value = remember
    }

    /**
     * Perform login.
     */
    fun login() {
        timber.log.Timber.tag("AUTH_DEBUG").d("EXPLICIT LOGIN BUTTON PRESSED")
        val email = _emailState.value.trim()
        val password = _passwordState.value
        val rememberMe = _rememberMeState.value

        when {
            email.isBlank() -> _errorState.value = "Email cannot be empty"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> _errorState.value = "Please enter a valid email address"
            password.isBlank() -> _errorState.value = "Password cannot be empty"
            else -> performLogin(email, password, rememberMe)
        }
    }

    /**
     * Perform actual login operation.
     */
    private fun performLogin(email: String, password: String, rememberMe: Boolean) {
        launchIO {
            _isLoadingState.value = true
            _errorState.value = null

            when (val result = loginUseCase(email, password)) {
                is Result.Success -> {
                    userPreferences.saveRememberMe(rememberMe, if (rememberMe) email else null)
                    _currentAdmin.value = result.data
                    _loginSuccessState.value = true
                }
                is Result.Error -> {
                    _errorState.value = result.message
                    _loginSuccessState.value = false
                }
                is Result.Loading -> {}
            }
            _isLoadingState.value = false
        }
    }

    /**
     * Handle forgot password.
     */
    fun forgotPassword() {
        val email = _emailState.value
        if (email.isBlank()) {
            _errorState.value = "Enter your email to reset password"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorState.value = "Please enter a valid email address"
            return
        }

        launchIO {
            _isLoadingState.value = true
            _errorState.value = null
            _successMessageState.value = null

            when (val result = forgotPasswordUseCase(email)) {
                is Result.Success -> {
                    _successMessageState.value = "Reset link sent to your email"
                    _showResetSuccessDialog.value = true
                }
                is Result.Error -> {
                    _errorState.value = result.message
                }
                is Result.Loading -> {}
            }
            _isLoadingState.value = false
        }
    }

    fun dismissResetSuccessDialog() {
        _showResetSuccessDialog.value = false
    }

    fun resetState() {
        _errorState.value = null
        _successMessageState.value = null
        _loginSuccessState.value = false
    }

    fun setError(message: String) {
        _errorState.value = message
        _loginSuccessState.value = false
    }
}

