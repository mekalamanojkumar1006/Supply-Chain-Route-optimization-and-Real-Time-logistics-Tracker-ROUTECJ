package com.routecj.customer.presentation.notifications

import com.routecj.customer.domain.model.CustomerNotification

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    object Empty : NotificationsUiState()
    data class Success(
        val notifications: List<CustomerNotification>,
        val unreadCount: Int
    ) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}
