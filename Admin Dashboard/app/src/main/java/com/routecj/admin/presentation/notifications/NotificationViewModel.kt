package com.routecj.admin.presentation.notifications

import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.security.SessionManager
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Notification
import com.routecj.admin.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase,
    private val sessionManager: SessionManager
) : BaseViewModel() {

    private val _notificationsState = MutableStateFlow<Result<List<Notification>>>(Result.Loading())
    val notificationsState: StateFlow<Result<List<Notification>>> = _notificationsState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<String?>(null)
    val typeFilter = _typeFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow<String?>(null)
    val priorityFilter = _priorityFilter.asStateFlow()

    val filteredNotifications = combine(
        _notificationsState,
        _searchQuery,
        _typeFilter,
        _priorityFilter
    ) { state, query, type, priority ->
        if (state is Result.Success) {
            var list = state.data
            if (query.isNotBlank()) {
                list = list.filter { it.title.contains(query, true) || it.message.contains(query, true) }
            }
            if (type != null) {
                list = list.filter { it.type.name == type }
            }
            if (priority != null) {
                list = list.filter { it.priority.name == priority }
            }
            Result.Success(list)
        } else state
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading())

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            sessionManager.currentAdmin.collect { admin ->
                if (admin != null) {
                    getNotificationsUseCase(admin.role, admin.uid).collect {
                        _notificationsState.value = it
                    }
                }
            }
        }
    }

    fun markAsRead(id: String) {
        launchIO { markNotificationReadUseCase(id) }
    }

    fun markAllAsRead() {
        val admin = sessionManager.currentAdmin.value ?: return
        launchIO { markAllNotificationsReadUseCase(admin.role, admin.uid) }
    }

    fun deleteNotification(id: String) {
        launchIO { deleteNotificationUseCase(id) }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setTypeFilter(t: String?) { _typeFilter.value = t }
    fun setPriorityFilter(p: String?) { _priorityFilter.value = p }
}
