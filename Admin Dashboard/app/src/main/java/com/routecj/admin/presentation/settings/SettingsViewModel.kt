package com.routecj.admin.presentation.settings

import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.security.SessionManager
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.repository.AuthRepository
import com.routecj.admin.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    val currentAdmin: StateFlow<Admin?> = sessionManager.currentAdmin

    private val _actionState = MutableStateFlow<Result<Unit>?>(null)
    val actionState: StateFlow<Result<Unit>?> = _actionState.asStateFlow()

    fun updateNotificationPreference(
        enabled: Boolean = currentAdmin.value?.notificationsEnabled ?: true,
        orderAlerts: Boolean = currentAdmin.value?.orderAlertsEnabled ?: true,
        dispatchAlerts: Boolean = currentAdmin.value?.dispatchAlertsEnabled ?: true,
        driverAlerts: Boolean = currentAdmin.value?.driverAlertsEnabled ?: true
    ) {
        val admin = currentAdmin.value ?: return
        val updatedAdmin = admin.copy(
            notificationsEnabled = enabled,
            orderAlertsEnabled = orderAlerts,
            dispatchAlertsEnabled = dispatchAlerts,
            driverAlertsEnabled = driverAlerts
        )
        
        launchIO {
            val result = profileRepository.updateAdminProfile(updatedAdmin)
            withMain {
                if (result is Result.Success) {
                    // Firestore snapshot listener will update currentAdmin via SessionManager
                } else if (result is Result.Error) {
                    _actionState.value = Result.Error(result.message)
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
}
