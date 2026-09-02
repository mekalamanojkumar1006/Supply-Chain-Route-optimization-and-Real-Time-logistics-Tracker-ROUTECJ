package com.routecj.driver.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.Driver
import com.routecj.driver.domain.usecase.GetCurrentDriverUseCase
import com.routecj.driver.domain.usecase.LoginDriverUseCase
import com.routecj.driver.domain.usecase.LogoutDriverUseCase
import com.routecj.driver.domain.usecase.SendPasswordResetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Initial : AuthUiState
    object CheckingSession : AuthUiState
    object Unauthenticated : AuthUiState
    object Loading : AuthUiState
    data class Authenticated(val driver: Driver) : AuthUiState
    data class DriverNotFound(val message: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
    data class PasswordResetSent(val message: String) : AuthUiState
}

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isForgotPasswordOpen: Boolean = false,
    val resetEmail: String = "",
    val resetEmailError: String? = null
)

class AuthViewModel(
    private val loginDriverUseCase: LoginDriverUseCase,
    private val getCurrentDriverUseCase: GetCurrentDriverUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    private val logoutDriverUseCase: LogoutDriverUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.CheckingSession)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    init {
        checkCurrentSession()
    }

    fun checkCurrentSession() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.CheckingSession
            when (val result = getCurrentDriverUseCase()) {
                is Result.Success -> {
                    _uiState.value = AuthUiState.Authenticated(result.data)
                }
                is Result.Error -> {
                    if (result.message.contains("DRIVER_ACCOUNT_NOT_FOUND", ignoreCase = true)) {
                        _uiState.value = AuthUiState.DriverNotFound(
                            "Your account is authenticated, but no authorized Driver profile was found."
                        )
                    } else {
                        _uiState.value = AuthUiState.Unauthenticated
                    }
                }
                is Result.Loading -> {
                    _uiState.value = AuthUiState.CheckingSession
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _formState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChanged(password: String) {
        _formState.update { it.copy(password = password, passwordError = null) }
    }

    fun onTogglePasswordVisibility() {
        _formState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onOpenForgotPassword(open: Boolean) {
        _formState.update { 
            it.copy(
                isForgotPasswordOpen = open,
                resetEmail = if (open) it.email else "",
                resetEmailError = null
            ) 
        }
    }

    fun onResetEmailChanged(email: String) {
        _formState.update { it.copy(resetEmail = email, resetEmailError = null) }
    }

    fun login() {
        val email = _formState.value.email.trim()
        val password = _formState.value.password

        var hasError = false
        if (email.isBlank()) {
            _formState.update { it.copy(emailError = "Enter your email.") }
            hasError = true
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _formState.update { it.copy(emailError = "Enter a valid email address.") }
            hasError = true
        }

        if (password.isBlank()) {
            _formState.update { it.copy(passwordError = "Enter your password.") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = loginDriverUseCase(email, password)) {
                is Result.Success -> {
                    _uiState.value = AuthUiState.Authenticated(result.data)
                }
                is Result.Error -> {
                    if (result.message.contains("DRIVER_ACCOUNT_NOT_FOUND", ignoreCase = true)) {
                        _uiState.value = AuthUiState.DriverNotFound(
                            "Your account is authenticated, but no authorized Driver profile was found."
                        )
                    } else {
                        _uiState.value = AuthUiState.Error(result.message)
                    }
                }
                is Result.Loading -> {
                    _uiState.value = AuthUiState.Loading
                }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _formState.value.resetEmail.trim()
        if (email.isBlank()) {
            _formState.update { it.copy(resetEmailError = "Enter your email.") }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _formState.update { it.copy(resetEmailError = "Enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = sendPasswordResetUseCase(email)) {
                is Result.Success -> {
                    _formState.update { it.copy(isForgotPasswordOpen = false) }
                    _uiState.value = AuthUiState.PasswordResetSent("Password reset email sent to $email.")
                }
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    fun logout() {
        logoutDriverUseCase()
        _formState.value = AuthFormState()
        _uiState.value = AuthUiState.Unauthenticated
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error || _uiState.value is AuthUiState.PasswordResetSent) {
            _uiState.value = AuthUiState.Unauthenticated
        }
    }
}
