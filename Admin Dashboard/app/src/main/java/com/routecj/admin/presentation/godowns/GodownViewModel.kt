package com.routecj.admin.presentation.godowns

import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.routecj.admin.core.presentation.BaseViewModel
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.domain.model.Godown
import com.routecj.admin.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class GodownViewModel @Inject constructor(
    private val getGodownsUseCase: GetGodownsUseCase,
    private val getGodownByIdUseCase: GetGodownByIdUseCase,
    private val createGodownUseCase: CreateGodownUseCase,
    private val updateGodownUseCase: UpdateGodownUseCase,
    private val deleteGodownUseCase: DeleteGodownUseCase,
    private val firestore: FirebaseFirestore
) : BaseViewModel() {

    private val _godownsState = MutableStateFlow<Result<List<Godown>>>(Result.Loading())
    val godownsState: StateFlow<Result<List<Godown>>> = _godownsState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter = _statusFilter.asStateFlow()

    val filteredGodowns = combine(
        _godownsState,
        _searchQuery,
        _statusFilter
    ) { state, query, status ->
        if (state is Result.Success) {
            var list = state.data
            if (query.isNotBlank()) {
                list = list.filter { it.name.contains(query, ignoreCase = true) || it.city.contains(query, ignoreCase = true) }
            }
            if (status != null) {
                list = list.filter { it.status.name.equals(status, ignoreCase = true) }
            }
            Result.Success(list)
        } else state
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Result.Loading())

    private val _actionState = MutableStateFlow<Result<Unit>?>(null)
    val actionState = _actionState.asStateFlow()

    private val _eligibleManagers = MutableStateFlow<List<Admin>>(emptyList())
    val eligibleManagers = _eligibleManagers.asStateFlow()

    init {
        loadGodowns()
        loadEligibleManagers()
    }

    private fun loadGodowns() {
        launchIO {
            getGodownsUseCase().collect { _godownsState.value = it }
        }
    }

    private fun loadEligibleManagers() {
        launchIO {
            try {
                // Fetch all admins and filter by Godown Manager role locally
                // Ideally this should be a query, but depends on how roles are stored (ID vs Name)
                val snapshot = firestore.collection("admins").get().await()
                val managers = snapshot.documents.mapNotNull { doc ->
                    val roleId = doc.getString("role")
                    if (AdminRole.fromId(roleId) == AdminRole.GODOWN_MANAGER) {
                        Admin(
                            uid = doc.getString("uid") ?: doc.id,
                            name = doc.getString("name") ?: "Unknown",
                            role = AdminRole.GODOWN_MANAGER
                        )
                    } else null
                }
                _eligibleManagers.value = managers
            } catch (e: Exception) {
                _eligibleManagers.value = emptyList()
            }
        }
    }

    fun createGodown(godown: Godown) {
        launchIO {
            _actionState.value = Result.Loading()
            _actionState.value = createGodownUseCase(godown)
        }
    }

    fun updateGodown(godown: Godown) {
        launchIO {
            _actionState.value = Result.Loading()
            _actionState.value = updateGodownUseCase(godown)
        }
    }

    fun deleteGodown(id: String) {
        launchIO {
            _actionState.value = Result.Loading()
            _actionState.value = deleteGodownUseCase(id)
        }
    }

    suspend fun getGodownById(id: String): Result<Godown> {
        val cached = (_godownsState.value as? Result.Success)?.data?.find { it.id == id }
        return if (cached != null) Result.Success(cached) else getGodownByIdUseCase(id)
    }

    fun clearActionState() { _actionState.value = null }
    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setStatusFilter(s: String?) { _statusFilter.value = s }
}
