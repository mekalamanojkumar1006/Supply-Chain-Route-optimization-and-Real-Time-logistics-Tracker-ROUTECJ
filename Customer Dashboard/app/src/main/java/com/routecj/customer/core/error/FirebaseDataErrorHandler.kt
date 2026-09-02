package com.routecj.customer.core.error

import com.google.firebase.firestore.FirebaseFirestoreException

fun Throwable.toDataError(): DataError {
    return when (this) {
        is FirebaseFirestoreException -> {
            when (this.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> DataError.PermissionDenied(this)
                FirebaseFirestoreException.Code.UNAVAILABLE -> DataError.NetworkUnavailable(this)
                FirebaseFirestoreException.Code.NOT_FOUND -> DataError.NotFound(this)
                else -> DataError.UnknownDataError(this.message ?: "Unknown Firestore Error", this)
            }
        }
        else -> DataError.UnknownDataError(this.message ?: "Unknown error", this)
    }
}
