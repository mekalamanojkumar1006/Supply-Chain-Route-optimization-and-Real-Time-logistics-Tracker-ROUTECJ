package com.routecj.driver.core.util

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[message=$message, throwable=$throwable]"
            Loading -> "Loading"
        }
    }
}
