package com.routecj.customer.presentation.profile

import com.routecj.customer.domain.model.Customer

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val customer: Customer) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class EditProfileState {
    object Idle : EditProfileState()
    object Updating : EditProfileState()
    object Success : EditProfileState()
    data class Error(val message: String) : EditProfileState()
}
