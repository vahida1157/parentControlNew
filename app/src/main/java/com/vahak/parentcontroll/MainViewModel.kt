package com.vahak.parentcontroll

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    sessionManager: SessionManager
) : ViewModel() {

    // A trigger to force a refresh when the Launcher Alias is toggled
    private val refreshTrigger = MutableStateFlow(0)

    val startDestination: StateFlow<String?> =
        combine(sessionManager.isLoggedIn, refreshTrigger) { isLoggedIn, _ ->
            val isLauncherActive = isLauncherAliasEnabled()

            when {
                !isLoggedIn -> Screen.Login.route
                isLauncherActive -> "child_launcher"
                else -> Screen.Dashboard.route
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun refreshDestination() {
        refreshTrigger.value += 1
    }

    private fun isLauncherAliasEnabled(): Boolean {
        val componentName = ComponentName(context, "${context.packageName}.ChildLauncherAlias")
        val state = context.packageManager.getComponentEnabledSetting(componentName)
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }
}