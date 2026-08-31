package com.routecj.admin.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/**
 * Manages user preferences using Jetpack DataStore.
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val SAVED_EMAIL = stringPreferencesKey("saved_email")
    }

    val rememberMe: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[REMEMBER_ME] ?: false
    }

    val savedEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SAVED_EMAIL]
    }

    suspend fun saveRememberMe(remember: Boolean, email: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[REMEMBER_ME] = remember
            if (remember && email != null) {
                preferences[SAVED_EMAIL] = email
            } else if (!remember) {
                preferences.remove(SAVED_EMAIL)
            }
        }
    }
}
