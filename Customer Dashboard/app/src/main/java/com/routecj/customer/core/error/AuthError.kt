package com.routecj.customer.core.error

sealed class AuthError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidCredentials(cause: Throwable? = null) : AuthError("Invalid email or password.", cause)
    class UserNotFound(cause: Throwable? = null) : AuthError("User not found.", cause)
    class UserDisabled(cause: Throwable? = null) : AuthError("User account disabled.", cause)
    class NetworkUnavailable(cause: Throwable? = null) : AuthError("Network unavailable.", cause)
    class UnknownAuthError(message: String, cause: Throwable? = null) : AuthError(message, cause)
}
