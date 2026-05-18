package com.vahak.parentcontroll.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme")
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val USER_PHONE = stringPreferencesKey("user_phone")
        private val ACTIVE_CHILD_ID = stringPreferencesKey("active_child_id")
        private val PARENT_PIN = stringPreferencesKey("parent_pin")
        private val SECURITY_QUESTION = stringPreferencesKey("security_question")
        private val SECURITY_ANSWER = stringPreferencesKey("security_answer")
        private val DEVICE_ID = stringPreferencesKey("device_id")
    }

    val appThemeFlow: Flow<AppTheme> = context.dataStore.data.map {
        AppTheme.valueOf(it[THEME_KEY] ?: AppTheme.SYSTEM.name)
    }
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    val authToken: Flow<String?> = context.dataStore.data.map { it[AUTH_TOKEN] }
    val userPhoneFlow: Flow<String?> = context.dataStore.data.map { it[USER_PHONE] }
    val activeChildIdFlow: Flow<String?> = context.dataStore.data.map { it[ACTIVE_CHILD_ID] }
    val parentPinFlow: Flow<String?> = context.dataStore.data.map { it[PARENT_PIN] }
    val securityQuestionFlow: Flow<String?> = context.dataStore.data.map { it[SECURITY_QUESTION] }
    val securityAnswerFlow: Flow<String?> = context.dataStore.data.map { it[SECURITY_ANSWER] }
    val deviceIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        var currentId = prefs[DEVICE_ID]
        if (currentId.isNullOrEmpty()) {
            currentId = java.util.UUID.randomUUID().toString()
            // Save it asynchronously so it's cached permanently
            CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                context.dataStore.edit { it[DEVICE_ID] = currentId }
            }
        }
        currentId
    }

    suspend fun saveSession(
        token: String,
        phone: String,
        pin: String?,
        securityQuestion: String?,
        securityAnswer: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = true
            prefs[AUTH_TOKEN] = token
            prefs[USER_PHONE] = phone
            pin?.let { prefs[PARENT_PIN] = it }
            securityQuestion?.let { prefs[SECURITY_QUESTION] = it }
            securityAnswer?.let { prefs[SECURITY_ANSWER] = it }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }

    suspend fun setActiveChildId(childId: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_CHILD_ID] = childId
        }
    }

    suspend fun clearActiveChildId() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACTIVE_CHILD_ID)
        }
    }

    suspend fun setParentPin(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[PARENT_PIN] = pin
        }
    }

    suspend fun hasParentPin(): Boolean {
        return !(parentPinFlow.first().isNullOrEmpty())
    }

    suspend fun clearParentPin() {
        context.dataStore.edit { prefs ->
            prefs.remove(PARENT_PIN)
        }
    }

    suspend fun setSecurityData(question: String, answer: String) {
        context.dataStore.edit { prefs ->
            prefs[SECURITY_QUESTION] = question
            prefs[SECURITY_ANSWER] = answer
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val prefs = context.dataStore.data.first()
        val currentId = prefs[DEVICE_ID]

        if (!currentId.isNullOrEmpty()) {
            return currentId
        }

        // Generate a new UUID if one doesn't exist
        val newId = java.util.UUID.randomUUID().toString()
        context.dataStore.edit { it[DEVICE_ID] = newId }
        return newId
    }

    suspend fun getDeviceName(): String {
        return android.os.Build.MODEL ?: "Unknown Android Device"
    }
}