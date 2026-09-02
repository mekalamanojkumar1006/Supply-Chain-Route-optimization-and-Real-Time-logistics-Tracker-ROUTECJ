package com.routecj.customer.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routecj.customer.domain.model.Customer
import com.routecj.customer.domain.repository.AuthRepository
import com.routecj.customer.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow<EditProfileState>(EditProfileState.Idle)
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private var currentCustomer: Customer? = null

    fun initProfile(customer: Customer) {
        currentCustomer = customer
        _name.value = customer.name ?: ""
        _phone.value = customer.phoneNumber ?: ""
        _state.value = EditProfileState.Idle
    }

    fun onNameChange(value: String) { _name.value = value }
    fun onPhoneChange(value: String) { _phone.value = value }

    fun saveProfile() {
        if (_name.value.isBlank()) {
            _state.value = EditProfileState.Error("Name cannot be empty.")
            return
        }

        val customer = currentCustomer ?: return
        
        _state.value = EditProfileState.Updating

        val updatedCustomer = customer.copy(
            name = _name.value,
            phoneNumber = _phone.value.takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            val result = customerRepository.updateCustomer(updatedCustomer)
            result.onSuccess {
                _state.value = EditProfileState.Success
            }.onFailure { error ->
                _state.value = EditProfileState.Error(error.message ?: "Failed to update profile.")
            }
        }
    }
}
