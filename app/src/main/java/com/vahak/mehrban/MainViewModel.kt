package com.vahak.mehrban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.uiv2.navigation.Screen
import com.vahak.mehrban.uiv2.theme.AppTheme // Make sure to import this
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    // 🚀 NEW: Expose the Theme Flow to MainActivity
    val appTheme: StateFlow<AppTheme> = sessionManager.appThemeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM // Default value
        )

    val activeChildId: StateFlow<String?> = sessionManager.activeChildIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val startDestination: StateFlow<String?> = sessionManager.isLoggedIn.map { isLoggedIn ->
        if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun clearActiveLauncherSession() {
        viewModelScope.launch {
            sessionManager.clearActiveChildId()
        }
    }
}