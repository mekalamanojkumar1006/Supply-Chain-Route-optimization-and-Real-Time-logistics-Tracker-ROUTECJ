package com.routecj.customer.domain.model

data class AuthResult(
    val uid: String,
    val email: String?,
    val isNewUser: Boolean
)
