package com.routecj.admin.core.util

/**
 * A sealed class that represents the result of an operation.
 * Following the Sealed Class pattern for type-safe handling of success/failure cases.
 * This promotes functional programming and makes error handling explicit.
 */
sealed class Result<T> {
    /**
     * Represents a successful operation with data.
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Represents a failed operation with error information.
     */
    data class Error<T>(
        val message: String,
        val code: Int? = null,
        val throwable: Throwable? = null
    ) : Result<T>()

    /**
     * Represents a loading state during an async operation.
     */
    class Loading<T> : Result<T>()
}

/**
 * Extension function to safely transform the result data.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(this.data))
        is Result.Error -> Result.Error(this.message, this.code, this.throwable)
        is Result.Loading -> Result.Loading()
    }
}

/**
 * Extension function to handle the result and perform an action.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(this.data)
    }
    return this
}

/**
 * Extension function to handle errors and perform an action.
 */
inline fun <T> Result<T>.onError(action: (String) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(this.message)
    }
    return this
}

/**
 * Extension function to check if the result is a success.
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Extension function to check if the result is an error.
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Extension function to check if the result is loading.
 */
fun <T> Result<T>.isLoading(): Boolean = this is Result.Loading

