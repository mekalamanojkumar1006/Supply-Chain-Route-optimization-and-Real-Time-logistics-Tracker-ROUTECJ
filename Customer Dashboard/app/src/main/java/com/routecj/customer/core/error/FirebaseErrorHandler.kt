package com.routecj.customer.core.error

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

fun Throwable.toAuthError(): AuthError {
    return when (this) {
        is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials(this)
        is FirebaseAuthUserCollisionException -> AuthError.UnknownAuthError("An account already exists with this email.", this)
        is FirebaseAuthInvalidUserException -> {
            if (this.errorCode == "ERROR_USER_DISABLED") {
                AuthError.UserDisabled(this)
            } else {
                AuthError.UserNotFound(this)
            }
        }
        is FirebaseAuthException -> {
            when (this.errorCode) {
                "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.NetworkUnavailable(this)
                else -> AuthError.UnknownAuthError(this.message ?: "Unknown Firebase Auth Error", this)
            }
        }
        else -> AuthError.UnknownAuthError(this.message ?: "Unknown error", this)
    }
}
