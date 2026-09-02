package com.routecj.customer.presentation.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.routecj.customer.domain.model.Customer
import com.routecj.customer.domain.repository.AuthRepository
import com.routecj.customer.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onFullNameChange(value: String) { _fullName.value = value; _errorMessage.value = null }
    fun onEmailChange(value: String) { _email.value = value; _errorMessage.value = null }
    fun onPhoneChange(value: String) { _phone.value = value; _errorMessage.value = null }
    fun onPasswordChange(value: String) { _password.value = value; _errorMessage.value = null }
    fun onConfirmPasswordChange(value: String) { _confirmPassword.value = value; _errorMessage.value = null }

    fun register() {
        if (_fullName.value.isBlank() || _email.value.isBlank() || _password.value.isBlank() || _confirmPassword.value.isBlank()) {
            _errorMessage.value = "Please fill all required fields."
            return
        }

        if (_password.value != _confirmPassword.value) {
            _errorMessage.value = "Passwords do not match."
            return
        }

        _isLoading.value = true
        _isSuccess.value = false
        _errorMessage.value = null

        viewModelScope.launch {
            Timber.tag("AUTH_REGISTER").d("Registration starting")
            val authResult = authRepository.signUpWithEmail(_email.value, _password.value)
            authResult.onSuccess { result ->
                val firebaseAuth = FirebaseAuth.getInstance()
                val currentUser = firebaseAuth.currentUser

                Timber.tag("AUTH_REGISTER").d("Auth creation SUCCESS")
                Timber.tag("AUTH_REGISTER").d("UID=${result.uid}")
                Timber.tag("AUTH_REGISTER").d("Firebase UID exists")
                if (currentUser != null && currentUser.uid == result.uid) {
                    Timber.tag("AUTH_REGISTER").d("currentUser != null")
                }

                if (currentUser == null || currentUser.uid != result.uid) {
                    Timber.tag("AUTH_REGISTER").e("Auth state not synchronized. currentUser is null or UID mismatch.")
                    _errorMessage.value = "Unable to create your customer profile. Please try again."
                    _isLoading.value = false
                    return@launch
                }

                val customer = Customer(
                    id = result.uid,
                    email = result.email ?: _email.value,
                    name = _fullName.value,
                    phoneNumber = _phone.value.takeIf { it.isNotBlank() },
                    role = "customer"
                )

                Timber.tag("AUTH_REGISTER").d("Starting Firestore profile creation")
                Timber.tag("AUTH_REGISTER").d("collection=customers")
                Timber.tag("AUTH_REGISTER").d("documentId=${customer.id}")
                Timber.tag("AUTH_REGISTER").d("role=customer")
                Timber.tag("AUTH_REGISTER").d("Firestore set() starting")

                val dbResult = customerRepository.createCustomer(customer)
                dbResult.onSuccess {
                    Timber.tag("AUTH_REGISTER").d("Firestore SUCCESS")
                    _isLoading.value = false
                    _isSuccess.value = true
                    _errorMessage.value = null
                }.onFailure { dbError ->
                    val firestoreException = dbError as? FirebaseFirestoreException
                        ?: dbError.cause as? FirebaseFirestoreException

                    if (firestoreException != null) {
                        Timber.tag("AUTH_REGISTER").e(
                            "Firestore FAILED exception=${firestoreException::class.java.name} code=${firestoreException.code} message=${firestoreException.message} cause=${firestoreException.cause}"
                        )
                    } else {
                        Timber.tag("AUTH_REGISTER").e(
                            "Firestore FAILED exception=${dbError::class.java.name} message=${dbError.message} cause=${dbError.cause}"
                        )
                    }

                    Timber.tag("AUTH_REGISTER").d("Diagnostic mode: skipping auth rollback to preserve the created user for Firestore diagnosis")
                    _errorMessage.value = "Unable to create your customer profile. Please try again."
                    _isLoading.value = false
                    _isSuccess.value = false
                }
            }.onFailure { authError ->
                Timber.tag("AUTH_REGISTER").e(authError, "Firebase Auth failed")
                _errorMessage.value = authError.message
                _isLoading.value = false
                _isSuccess.value = false
            }
        }
    }
}
