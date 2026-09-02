package com.routecj.customer.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.customer.data.local.ThemePreferences
import com.routecj.customer.domain.repository.AuthRepository
import com.routecj.customer.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    val isDarkMode: StateFlow<Boolean> = themePreferences.isDarkMode

    init {
        loadProfile()
    }

    fun loadProfile() {
        _state.value = ProfileState.Loading
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            _state.value = ProfileState.Error("User not authenticated.")
            return
        }

        viewModelScope.launch {
            val result = customerRepository.getCustomer(userId)
            result.onSuccess { customer ->
                _state.value = ProfileState.Success(customer)
            }.onFailure { error ->
                _state.value = ProfileState.Error(error.message ?: "Failed to load profile.")
            }
        }
    }

    fun toggleDarkMode() {
        themePreferences.toggleDarkMode()
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
