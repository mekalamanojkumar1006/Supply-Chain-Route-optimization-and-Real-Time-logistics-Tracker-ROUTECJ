package com.routecj.admin.data.repository

import com.routecj.admin.core.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Base repository class that implements common repository patterns.
 * Provides reusable methods for handling API calls and database operations.
 *
 * Responsibilities:
 * - Abstracting data sources (remote and local)
 * - Handling API response mapping
 * - Error handling and logging
 * - Enforcing single source of truth pattern
 */
abstract class BaseRepository {

    /**
     * Safely executes an API call and returns a Result object.
     * Handles common errors like network issues, parsing errors, and HTTP errors.
     *
     * @param T - Type of the data returned by the API
     * @param apiCall - The suspend function that makes the API call
     * @return Result<T> - Success, Error, or Loading state
     */
    protected suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val response = apiCall()

            when {
                response.isSuccessful -> {
                    val body = response.body()
                    if (body != null) {
                        Result.Success(body)
                    } else {
                        Result.Error("Empty response body")
                    }
                }
                response.code() == 401 -> {
                    Result.Error("Unauthorized", response.code())
                }
                response.code() == 403 -> {
                    Result.Error("Forbidden", response.code())
                }
                response.code() == 404 -> {
                    Result.Error("Not found", response.code())
                }
                response.code() >= 500 -> {
                    Result.Error("Server error", response.code())
                }
                else -> {
                    Result.Error("Unknown error: ${response.message()}", response.code())
                }
            }
        } catch (e: Exception) {
            Result.Error(
                message = e.message ?: "Unknown error",
                throwable = e
            )
        }
    }

    /**
     * Safely executes a database operation and returns a Result object.
     * Handles database-specific errors.
     *
     * @param T - Type of the data returned by the database
     * @param dbCall - The suspend function that performs the database operation
     * @return Result<T> - Success or Error state
     */
    protected suspend fun <T> safeDbCall(
        dbCall: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            val result = dbCall()
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error(
                message = "Database error: ${e.message ?: "Unknown error"}",
                throwable = e
            )
        }
    }

    /**
     * Safely executes a general operation and returns a Result object.
     * Useful for operations that don't fit into API or DB categories.
     *
     * @param T - Type of the data returned
     * @param operation - The suspend function that performs the operation
     * @return Result<T> - Success or Error state
     */
    protected suspend fun <T> safeCall(
        operation: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.Success(operation())
        } catch (e: Exception) {
            Result.Error(
                message = e.message ?: "Unknown error",
                throwable = e
            )
        }
    }
}

