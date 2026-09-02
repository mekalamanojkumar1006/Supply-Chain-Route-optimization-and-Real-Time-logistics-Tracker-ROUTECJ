package com.routecj.driver.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.driver.core.util.Result
import com.routecj.driver.domain.model.DriverNotification
import com.routecj.driver.domain.model.NotificationFilter
import com.routecj.driver.domain.repository.NotificationRepository
import com.routecj.driver.domain.usecase.GetDriverNotificationsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NotificationUiState {
    object Loading : NotificationUiState
    data class Success(
        val notifications: List<DriverNotification>,
        val currentFilter: NotificationFilter,
        val totalCount: Int,
        val unreadCount: Int
    ) : NotificationUiState
    data class Error(val message: String) : NotificationUiState
}

class NotificationViewModel(
    private val getDriverNotificationsUseCase: GetDriverNotificationsUseCase,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(NotificationFilter.ALL)
    val selectedFilter: StateFlow<NotificationFilter> = _selectedFilter.asStateFlow()

    private val _unreadBadgeCount = MutableStateFlow(0)
    val unreadBadgeCount: StateFlow<Int> = _unreadBadgeCount.asStateFlow()

    private var observationJob: Job? = null
    private var badgeJob: Job? = null
    private var currentDriverId: String = ""
    private var isMarkingAllRead = false

    fun initialize(driverId: String) {
        if (driverId.isBlank() || (currentDriverId == driverId && observationJob != null)) return
        currentDriverId = driverId
        observeNotifications()
        observeBadgeCount()
    }

    fun setFilter(filter: NotificationFilter) {
        _selectedFilter.value = filter
        observeNotifications()
    }

    fun markAsRead(notificationId: String) {
        if (notificationId.isBlank()) return
        viewModelScope.launch {
            notificationRepository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        if (currentDriverId.isBlank() || isMarkingAllRead) return
        isMarkingAllRead = true
        viewModelScope.launch {
            try {
                notificationRepository.markAllNotificationsAsRead(currentDriverId)
            } finally {
                isMarkingAllRead = false
            }
        }
    }

    fun retry() {
        observeNotifications()
    }

    private fun observeBadgeCount() {
        badgeJob?.cancel()
        badgeJob = viewModelScope.launch {
            notificationRepository.observeUnreadCount(currentDriverId).collect { count ->
                _unreadBadgeCount.value = count
            }
        }
    }

    private fun observeNotifications() {
        if (currentDriverId.isBlank()) return
        observationJob?.cancel()
        _uiState.value = NotificationUiState.Loading

        observationJob = viewModelScope.launch {
            getDriverNotificationsUseCase(currentDriverId, NotificationFilter.ALL).collect { allResult ->
                when (allResult) {
                    is Result.Success -> {
                        val allList = allResult.data
                        val unreadCount = allList.count { !it.isRead }
                        val filteredList = when (_selectedFilter.value) {
                            NotificationFilter.ALL -> allList
                            NotificationFilter.UNREAD -> allList.filter { !it.isRead }
                        }

                        _uiState.value = NotificationUiState.Success(
                            notifications = filteredList,
                            currentFilter = _selectedFilter.value,
                            totalCount = allList.size,
                            unreadCount = unreadCount
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = NotificationUiState.Error(
                            allResult.message.ifBlank { "Unable to load notifications. Please check your connection." }
                        )
                    }
                    is Result.Loading -> {
                        _uiState.value = NotificationUiState.Loading
                    }
                }
            }
        }
    }
}
