package com.routecj.admin.presentation.profile

import android.util.Log
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.repository.AuthRepository
import com.routecj.admin.domain.usecase.GetCurrentAdminProfileUseCase
import com.routecj.admin.domain.usecase.UpdateAdminProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Profile ViewModel.
 * Manages profile data loading and display state.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentAdminProfileUseCase: GetCurrentAdminProfileUseCase,
    private val updateAdminProfileUseCase: UpdateAdminProfileUseCase,
    private val authRepository: AuthRepository
) : BaseViewModel() {

    // Loading state for initial load
    private val _isLoadingState = MutableStateFlow(true)
    val isLoadingState: StateFlow<Boolean> = _isLoadingState.asStateFlow()

    // Error state for initial load
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    // Action state for updates (Loading, Success, Error)
    private val _actionState = MutableStateFlow<Result<Unit>?>(null)
    val actionState: StateFlow<Result<Unit>?> = _actionState.asStateFlow()

    // Current admin profile data
    private val _adminProfile = MutableStateFlow<Admin?>(null)
    val adminProfile: StateFlow<Admin?> = _adminProfile.asStateFlow()

    init {
        loadAdminProfile()
    }

    /**
     * Load current admin's profile from Firestore.
     */
    private fun loadAdminProfile() {
        launchIO {
            _isLoadingState.value = true
            _errorState.value = null

            Log.d("PROFILE_VM", "Loading admin profile...")
            when (val result = getCurrentAdminProfileUseCase()) {
                is Result.Success -> {
                    _adminProfile.value = result.data
                    _isLoadingState.value = false
                }
                is Result.Error -> {
                    _errorState.value = result.message
                    _isLoadingState.value = false
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Update the admin's profile.
     */
    fun updateProfile(name: String, phone: String, profileImage: String?) {
        val current = _adminProfile.value ?: return
        val updated = current.copy(name = name, phone = phone, profileImage = profileImage)

        launchIO {
            _actionState.value = Result.Loading()
            val result = updateAdminProfileUseCase(updated)
            withMain {
                _actionState.value = result
                if (result is Result.Success) {
                    _adminProfile.value = updated
                }
            }
        }
    }

    fun changePassword(current: String, new: String) {
        launchIO {
            _actionState.value = Result.Loading()
            val result = authRepository.changePassword(current, new)
            withMain {
                _actionState.value = result
            }
        }
    }

    fun logout() {
        launchIO {
            authRepository.logout()
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }

    /**
     * Retry loading the profile.
     */
    fun retryLoadProfile() {
        loadAdminProfile()
    }
}
