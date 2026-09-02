package com.routecj.customer.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.customer.domain.repository.AuthRepository
import com.routecj.customer.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private val customerId: String? = authRepository.getCurrentUserId()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        val uid = customerId ?: run {
            _state.value = NotificationsUiState.Error("User not authenticated.")
            return
        }

        viewModelScope.launch {
            _state.value = NotificationsUiState.Loading

            combine(
                notificationRepository.getNotificationsFlow(uid),
                notificationRepository.getUnreadCountFlow(uid)
            ) { notifResult, unreadCount ->
                Pair(notifResult, unreadCount)
            }
                .catch { e ->
                    timber.log.Timber.e(e, "Notifications flow failed")
                    _state.value = NotificationsUiState.Error("Unable to load notifications. Please try again.")
                }
                .collect { (notifResult, unreadCount) ->
                    notifResult.onSuccess { list ->
                        _state.value = if (list.isEmpty()) {
                            NotificationsUiState.Empty
                        } else {
                            NotificationsUiState.Success(list, unreadCount)
                        }
                    }.onFailure { e ->
                        timber.log.Timber.e(e, "Notification repository failure")
                        _state.value = NotificationsUiState.Error("Unable to load notifications. Please try again.")
                    }
                }
        }
    }

    fun markAsRead(notificationId: String) {
        val uid = customerId ?: return
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId, uid)
        }
    }

    fun markAllAsRead() {
        val uid = customerId ?: return
        viewModelScope.launch {
            notificationRepository.markAllAsRead(uid)
        }
    }
}
