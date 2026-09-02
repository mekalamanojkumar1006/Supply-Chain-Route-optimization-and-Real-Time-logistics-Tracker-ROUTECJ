package com.routecj.customer.domain.model

data class Customer(
    val id: String,
    val email: String,
    val name: String?,
    val phoneNumber: String?,
    val role: String,
    val isActiveAccount: Boolean = true,
    val profileImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
