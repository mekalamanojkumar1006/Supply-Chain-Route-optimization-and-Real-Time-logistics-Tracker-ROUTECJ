package com.routecj.customer.core.error

sealed class DataError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkUnavailable(cause: Throwable? = null) : DataError("Network unavailable.", cause)
    class PermissionDenied(cause: Throwable? = null) : DataError("Permission denied.", cause)
    class NotFound(cause: Throwable? = null) : DataError("Data not found.", cause)
    class UnknownDataError(message: String, cause: Throwable? = null) : DataError(message, cause)
}
