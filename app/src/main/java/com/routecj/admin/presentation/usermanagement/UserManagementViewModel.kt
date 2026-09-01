package com.routecj.admin.presentation.usermanagement

import androidx.lifecycle.viewModelScope
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.util.EmailUtils
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.DriverStatus
import com.routecj.admin.domain.usecase.CheckDuplicateAccountUseCase
import com.routecj.admin.domain.usecase.CreateAdminAccountUseCase
import com.routecj.admin.domain.usecase.CreateDriverAccountUseCase
import com.routecj.admin.domain.usecase.GetAdminUsersUseCase
import com.routecj.admin.domain.usecase.GetDriversUseCase
import com.routecj.admin.domain.usecase.UpdateUserStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UserManagementTab {
    ADMINS,
    DRIVERS
}

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val getAdminUsersUseCase: GetAdminUsersUseCase,
    private val getDriversUseCase: GetDriversUseCase,
    private val createDriverAccountUseCase: CreateDriverAccountUseCase,
    private val createAdminAccountUseCase: CreateAdminAccountUseCase,
    private val updateUserStatusUseCase: UpdateUserStatusUseCase,
    private val checkDuplicateAccountUseCase: CheckDuplicateAccountUseCase
) : BaseViewModel() {

    private val _selectedTab = MutableStateFlow(UserManagementTab.ADMINS)
    val selectedTab: StateFlow<UserManagementTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _roleFilter = MutableStateFlow<AdminRole?>(null)
    val roleFilter: StateFlow<AdminRole?> = _roleFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter: StateFlow<String?> = _statusFilter.asStateFlow()

    private val _adminsState = MutableStateFlow<Result<List<Admin>>>(Result.Loading())
    val adminsState: StateFlow<Result<List<Admin>>> = _adminsState.asStateFlow()

    private val _driversState = MutableStateFlow<Result<List<Driver>>>(Result.Loading())
    val driversState: StateFlow<Result<List<Driver>>> = _driversState.asStateFlow()

    private val _actionState = MutableStateFlow<Result<Unit>?>(null)
    val actionState: StateFlow<Result<Unit>?> = _actionState.asStateFlow()

    val filteredAdmins = combine(_adminsState, _searchQuery, _roleFilter, _statusFilter) { adminsRes, query, role, status ->
        when (adminsRes) {
            is Result.Loading -> Result.Loading()
            is Result.Error -> adminsRes
            is Result.Success -> {
                val filtered = adminsRes.data.filter { admin ->
                    val matchesQuery = query.isBlank() ||
                            admin.name.contains(query, ignoreCase = true) ||
                            admin.email.contains(query, ignoreCase = true) ||
                            admin.phone.contains(query, ignoreCase = true)

                    val matchesRole = role == null || admin.role == role
                    val matchesStatus = status == null || admin.status.equals(status, ignoreCase = true)

                    matchesQuery && matchesRole && matchesStatus
                }
                Result.Success(filtered)
            }
        }
    }

    val filteredDrivers = combine(_driversState, _searchQuery, _statusFilter) { driversRes, query, status ->
        when (driversRes) {
            is Result.Loading -> Result.Loading()
            is Result.Error -> driversRes
            is Result.Success -> {
                val filtered = driversRes.data.filter { driver ->
                    val matchesQuery = query.isBlank() ||
                            driver.name.contains(query, ignoreCase = true) ||
                            driver.email.contains(query, ignoreCase = true) ||
                            driver.phone.contains(query, ignoreCase = true) ||
                            driver.id.contains(query, ignoreCase = true)

                    val matchesStatus = status == null || driver.status.name.equals(status, ignoreCase = true)

                    matchesQuery && matchesStatus
                }
                Result.Success(filtered)
            }
        }
    }

    init {
        loadData()
    }

    fun setTab(tab: UserManagementTab) {
        _selectedTab.value = tab
        _searchQuery.value = ""
        _roleFilter.value = null
        _statusFilter.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRoleFilter(role: AdminRole?) {
        _roleFilter.value = role
    }

    fun setStatusFilter(status: String?) {
        _statusFilter.value = status
    }

    fun loadData() {
        launchIO {
            getAdminUsersUseCase().collect { result ->
                withMain { _adminsState.value = result }
            }
        }
        launchIO {
            getDriversUseCase().collect { result ->
                withMain { _driversState.value = result }
            }
        }
    }

    fun createDriverAccount(driver: Driver, tempPassword: String) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }
            val result = createDriverAccountUseCase(driver, tempPassword)
            withMain {
                _actionState.value = when (result) {
                    is Result.Success -> Result.Success(Unit)
                    is Result.Error -> Result.Error(result.message, result.code, result.throwable)
                    is Result.Loading -> Result.Loading()
                }
            }
        }
    }

    fun createAdminAccount(admin: Admin, tempPassword: String) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }
            val result = createAdminAccountUseCase(admin, tempPassword)
            withMain {
                _actionState.value = when (result) {
                    is Result.Success -> Result.Success(Unit)
                    is Result.Error -> Result.Error(result.message, result.code, result.throwable)
                    is Result.Loading -> Result.Loading()
                }
            }
        }
    }

    fun updateAdminStatus(adminId: String, status: String) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }
            val result = updateUserStatusUseCase.updateAdminStatus(adminId, status)
            withMain { _actionState.value = result }
        }
    }

    fun updateDriverStatus(driverId: String, status: DriverStatus, isActive: Boolean) {
        launchIO {
            withMain { _actionState.value = Result.Loading() }
            val result = updateUserStatusUseCase.updateDriverStatus(driverId, status, isActive)
            withMain { _actionState.value = result }
        }
    }

    fun clearActionState() {
        _actionState.value = null
    }
}
