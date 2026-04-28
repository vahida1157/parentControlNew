package com.vahak.parentcontroll

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.ui.navigation.Screen
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

    // 🚀 NEW: Expose the Master Switch directly to MainActivity
    val activeChildId: StateFlow<String?> = sessionManager.activeChildIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Simplified: Just handles Login vs Dashboard.
    // It no longer cares about the Launcher alias.
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