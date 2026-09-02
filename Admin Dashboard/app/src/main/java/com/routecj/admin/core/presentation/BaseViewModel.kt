package com.routecj.admin.core.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base ViewModel class for all ViewModels in the application.
 * Provides common functionality and enforces architectural patterns.
 *
 * Benefits:
 * - Centralized lifecycle management
 * - Common coroutine scope handling
 * - Error handling patterns
 * - Logging and debugging capabilities
 */
abstract class BaseViewModel : ViewModel() {

    /**
     * Gets the CoroutineScope for the ViewModel.
     * Lifecycle is automatically managed by the framework.
     */
    protected val vmScope: CoroutineScope
        get() = viewModelScope

    /**
     * Launches a coroutine in the ViewModel scope with IO dispatcher.
     * Suitable for network and database operations.
     */
    protected fun launchIO(block: suspend () -> Unit) {
        vmScope.launch(Dispatchers.IO) {
            try {
                block()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Launches a coroutine in the ViewModel scope with Main dispatcher.
     * Suitable for UI updates.
     */
    protected fun launchMain(block: suspend () -> Unit) {
        vmScope.launch(Dispatchers.Main) {
            try {
                block()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    /**
     * Switches context from one dispatcher to another.
     * Useful for transitioning from IO to Main operations.
     */
    protected suspend fun <T> withIO(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            block()
        }
    }

    /**
     * Switches context to Main dispatcher.
     * Used for UI-related operations.
     */
    protected suspend fun <T> withMain(block: suspend () -> T): T {
        return withContext(Dispatchers.Main) {
            block()
        }
    }

    /**
     * Override this method in child classes to handle errors.
     * Provides a centralized error handling mechanism.
     */
    protected open fun handleError(exception: Exception) {
        // Override in child classes for specific error handling
        exception.printStackTrace()
    }
}

