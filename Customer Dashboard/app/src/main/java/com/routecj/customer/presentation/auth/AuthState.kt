package com.routecj.customer.presentation.auth

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    object Authenticated : AuthState()
    data class Unauthorized(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
