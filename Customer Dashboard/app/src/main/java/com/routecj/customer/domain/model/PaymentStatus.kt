package com.routecj.customer.domain.model

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED;

    companion object {
        fun fromString(value: String?): PaymentStatus =
            values().firstOrNull { it.name == value } ?: PENDING
    }
}
