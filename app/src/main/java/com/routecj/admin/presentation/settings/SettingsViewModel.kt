package com.routecj.admin.presentation.settings

import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.security.SessionManager
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.repository.AuthRepository
import com.routecj.admin.domain.repository.ProfileRepository
import com.routecj.admin.domain.model.BackupStatus
import com.routecj.admin.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val backupRepository: BackupRepository,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    val currentAdmin: StateFlow<Admin?> = sessionManager.currentAdmin

    val backupStatus: StateFlow<Result<BackupStatus>> = backupRepository.observeBackupStatus()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Result.Loading()
        )

    private val _syncState = MutableStateFlow<Result<BackupStatus>?>(null)
    val syncState: StateFlow<Result<BackupStatus>?> = _syncState.asStateFlow()

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

    fun triggerBackupSync() {
        launchIO {
            _syncState.value = Result.Loading()
            val result = backupRepository.triggerBackupSync()
            withMain {
                _syncState.value = result
            }
        }
    }

    fun clearSyncState() {
        _syncState.value = null
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
