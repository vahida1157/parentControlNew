package com.vahak.parentcontroll.presentation.launcher

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.core.data.local.dao.AppRuleDao
import com.vahak.parentcontroll.core.data.local.dao.ChildSettingsDao
import com.vahak.parentcontroll.core.data.local.dao.UsageDao
import com.vahak.parentcontroll.core.data.local.entity.Gender
import com.vahak.parentcontroll.core.util.AppInfo
import com.vahak.parentcontroll.core.util.AppManager
import com.vahak.parentcontroll.domain.repository.ChildRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class LauncherStateV2(
    val childName: String = "",
    val gender: Gender = Gender.BOY,
    val installedApps: List<AppInfo> = emptyList(),
    val usageSeconds: Int = 0,
    val timeLimitMins: Int = 0,
    val isTimeLimitActive: Boolean = false,

    val isLoading: Boolean = true,

    val showExitDialog: Boolean = false,
    val enteredPin: String = "",
    val pinError: Boolean = false
)

sealed class LauncherEventV2 {
    data class AppClicked(val packageName: String) : LauncherEventV2()

    object ExitLauncherClicked : LauncherEventV2()
    object DismissExitDialog : LauncherEventV2()
    data class PinDigitEntered(val digit: String) : LauncherEventV2()
    object PinBackspaceClicked : LauncherEventV2()
    data class SubmitExitPin(val pin: String) : LauncherEventV2()
}

sealed class LauncherEffectV2 {
    object RequestExit : LauncherEffectV2()
    data class ShowToast(val icon: String, val message: String) : LauncherEffectV2()
}

@HiltViewModel
class LauncherViewModelV2 @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRuleDao: AppRuleDao,
    private val sessionManager: SessionManager,
    private val childRepository: ChildRepository,
    private val settingsDao: ChildSettingsDao,
    private val usageDao: UsageDao
) : BaseViewModel<LauncherStateV2, LauncherEventV2, LauncherEffectV2>(LauncherStateV2()) {

    init {
        observeActiveSession()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeActiveSession() {
        val activeChildIdFlow = sessionManager.activeChildIdFlow.distinctUntilChanged()

        // 1. Observe Child Details
        viewModelScope.launch(Dispatchers.IO) {
            activeChildIdFlow.flatMapLatest { childId ->
                if (childId != null) childRepository.observeChildById(childId) else flowOf(null)
            }.collectLatest { child ->
                if (child != null) {
                    updateState { copy(childName = child.name, gender = child.gender) }
                }
            }
        }

        // 2. Observe Apps
        viewModelScope.launch(Dispatchers.IO) {
            activeChildIdFlow.flatMapLatest { childId ->
                if (childId != null) appRuleDao.observeAllowedApps(childId) else flowOf(emptyList())
            }.collectLatest { rules ->
                val allowedPackages = rules.map { it.packageName }.toSet()
                val specificApps = AppManager.getSpecificApps(context, allowedPackages)
                updateState { copy(installedApps = specificApps, isLoading = false) }
            }
        }

        // 3. Observe Time Limits
        viewModelScope.launch(Dispatchers.IO) {
            activeChildIdFlow.flatMapLatest { childId ->
                if (childId != null) settingsDao.getGlobalSettings(childId) else flowOf(null)
            }.collectLatest { settings ->
                updateState {
                    copy(
                        timeLimitMins = settings?.dailyTimeLimitMins ?: 0,
                        isTimeLimitActive = settings?.isTimeLimitActive ?: false
                    )
                }
            }
        }

        // 4. Observe Daily Usage (🚀 FIXED: Uses maxOf to prevent bypassing global limits)
        viewModelScope.launch(Dispatchers.IO) {
            activeChildIdFlow.flatMapLatest { childId ->
                if (childId != null) usageDao.observeDailyUsage(childId, LocalDate.now()) else flowOf(null)
            }.collectLatest { daily ->
                val localSecs = daily?.usedSeconds ?: 0
                val cachedGlobalSecs = daily?.globalUsedSeconds ?: 0
                updateState { copy(usageSeconds = maxOf(localSecs, cachedGlobalSecs)) }
            }
        }
    }

    override fun onEvent(event: LauncherEventV2) {
        when (event) {
            is LauncherEventV2.AppClicked -> {
                // Since usageSeconds is now the global effective time, this block works perfectly!
                if (state.value.isTimeLimitActive && state.value.timeLimitMins > 0) {
                    val limitSecs = state.value.timeLimitMins * 60
                    if (state.value.usageSeconds >= limitSecs) {
                        sendEffect(LauncherEffectV2.ShowToast("⏳", "زمان استفاده شما به پایان رسیده است"))
                        return
                    }
                }
                AppManager.launchApp(context, event.packageName)
            }

            is LauncherEventV2.ExitLauncherClicked -> {
                updateState { copy(showExitDialog = true, enteredPin = "", pinError = false) }
            }

            is LauncherEventV2.DismissExitDialog -> {
                updateState { copy(showExitDialog = false, enteredPin = "", pinError = false) }
            }

            is LauncherEventV2.PinDigitEntered -> {
                val currentPin = state.value.enteredPin
                if (currentPin.length < 8) {
                    updateState { copy(enteredPin = currentPin + event.digit, pinError = false) }
                }
            }

            is LauncherEventV2.PinBackspaceClicked -> {
                val currentPin = state.value.enteredPin
                if (currentPin.isNotEmpty()) {
                    updateState { copy(enteredPin = currentPin.dropLast(1), pinError = false) }
                }
            }

            is LauncherEventV2.SubmitExitPin -> {
                verifyPin(event.pin)
            }
        }
    }

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            val savedPin = sessionManager.parentPinFlow.first()
            if (savedPin.isNullOrEmpty() || pin == savedPin) {
                updateState { copy(showExitDialog = false, enteredPin = "") }
                sendEffect(LauncherEffectV2.RequestExit)

            } else {
                updateState { copy(pinError = true) }
                delay(600) // Shake animation duration
                updateState { copy(enteredPin = "", pinError = false) }
            }
        }
    }
}