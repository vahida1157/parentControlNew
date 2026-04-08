package com.vahak.parentcontroll.presentation.launcher

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.core.data.local.dao.AppRuleDao
import com.vahak.parentcontroll.core.util.AppInfo
import com.vahak.parentcontroll.core.util.AppManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRuleDao: AppRuleDao,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeActiveSession()
    }

    private fun observeActiveSession() {
        viewModelScope.launch(Dispatchers.IO) {
            // Watch the SessionManager. If the child ID changes, reload the apps!
            sessionManager.activeChildIdFlow.distinctUntilChanged().collectLatest { childId ->
                if (childId != null) {
                    loadAppsForChild(childId)
                } else {
                    // Safety fallback if no child is active
                    _installedApps.value = emptyList()
                    _isLoading.value = false
                }
            }
        }
    }

    private suspend fun loadAppsForChild(childId: String) {
        _isLoading.value = true

        // Listen to the database for this specific child
        appRuleDao.observeAllowedApps(childId).distinctUntilChanged().collectLatest { rules ->
            val allowedPackages = rules.map { it.packageName }.toSet()

            // Only load the specific icons the parent allowed
            val specificApps = AppManager.getSpecificApps(context, allowedPackages)

            _installedApps.value = specificApps
            _isLoading.value = false
        }
    }

    fun launchApp(packageName: String) {
        AppManager.launchApp(context, packageName)
    }
}