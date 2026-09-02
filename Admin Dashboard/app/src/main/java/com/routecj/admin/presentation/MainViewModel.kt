package com.routecj.admin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.routecj.admin.core.security.SessionManager
import com.routecj.admin.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Global ViewModel for managing application-wide state like authentication session.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private var profileJob: kotlinx.coroutines.Job? = null

    init {
        firebaseAuth.addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            if (uid != null) {
                startObservingProfile(uid)
            } else {
                stopObservingProfile()
            }
        }
    }

    private fun startObservingProfile(uid: String) {
        profileJob?.cancel()
        profileJob = authRepository.observeAdminProfile(uid)
            .launchIn(viewModelScope)
    }

    private fun stopObservingProfile() {
        profileJob?.cancel()
        profileJob = null
        sessionManager.clearSession()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    val currentAdmin = sessionManager.currentAdmin
}
