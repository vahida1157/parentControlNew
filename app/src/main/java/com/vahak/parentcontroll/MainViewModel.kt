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
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    sessionManager: SessionManager
) : ViewModel() {

    // Reads DataStore and determines the start destination.
    // Returns null while it is still loading from disk.
    val startDestination: StateFlow<String?> = sessionManager.isLoggedIn.map { isLoggedIn ->
        if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
}