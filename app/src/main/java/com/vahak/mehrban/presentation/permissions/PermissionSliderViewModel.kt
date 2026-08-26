package com.vahak.mehrban.presentation.permissions

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.analytics.AppAnalytics
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.util.PermissionChecker
import com.vahak.mehrban.core.util.PermissionType
import com.vahak.mehrban.presentation.BaseViewModel
import com.vahak.mehrban.presentation.permissions.PermissionSliderEffect.LaunchAndroidSettings
import com.vahak.mehrban.presentation.permissions.PermissionSliderEffect.NavigateToFeature
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class PermissionSliderState(
    val missingPermissions: List<PermissionType> = emptyList(), val targetFeatureRoute: String = ""
)

sealed class PermissionSliderEvent {
    data class GrantClicked(val permission: PermissionType) : PermissionSliderEvent()
    data class SkipClicked(val permission: PermissionType) : PermissionSliderEvent()
    object SetupFinished : PermissionSliderEvent()
    data class CheckPermissions(val context: Context) : PermissionSliderEvent()
}

sealed class PermissionSliderEffect {
    data class LaunchAndroidSettings(val action: String, val permission: PermissionType) :
        PermissionSliderEffect()

    data class NavigateToFeature(val route: String) : PermissionSliderEffect()
}

@HiltViewModel
class PermissionSliderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analytics: AppAnalytics,
    private val sessionManager: SessionManager,
) : BaseViewModel<PermissionSliderState, PermissionSliderEvent, PermissionSliderEffect>(
    PermissionSliderState()
) {

    init {
        val route = savedStateHandle.get<String>("featureRoute") ?: ""
        val permissionsString = savedStateHandle.get<String>("missingPermissions") ?: ""

        val permissionsList = if (permissionsString.isNotBlank()) {
            permissionsString.split(",").mapNotNull { name ->
                runCatching { PermissionType.valueOf(name) }.getOrNull()
            }
        } else emptyList()

        Timber.d(
            "Initializing permission slider, missingCount: %d, targetRoute: %s",
            permissionsList.size,
            route
        )
        analytics.logPermissionSetupStarted(route, permissionsList.size)
        updateState {
            copy(targetFeatureRoute = route, missingPermissions = permissionsList)
        }
    }

    override fun onEvent(event: PermissionSliderEvent) {
        when (event) {
            is PermissionSliderEvent.GrantClicked -> {
                Timber.d("User initiated OS permission request: %s", event.permission)
                analytics.logPermissionStepClicked(event.permission.name)
                sendEffect(
                    LaunchAndroidSettings(
                        action = event.permission.androidSettingsAction,
                        permission = event.permission
                    )
                )
            }

            is PermissionSliderEvent.CheckPermissions -> {
                // 1. Cache the old list BEFORE we do any math or state updates
                val oldMissing = state.value.missingPermissions

                // 2. Find out which ones are STILL missing
                val stillMissing = oldMissing.filter {
                    !PermissionChecker.hasPermission(event.context, it)
                }

                // 3. Find out which ones were just granted
                val newlyGranted = oldMissing - stillMissing.toSet()

                // 4. Log success
                newlyGranted.forEach { permission ->
                    Timber.d("Detected OS permission newly granted: %s", permission.name)
                    analytics.logPermissionStepSuccess(permission.name)
                }

                // 5. Update the UI state so the slider moves forward (or disappears)
                updateState { copy(missingPermissions = stillMissing) }

                // 🚀 THE FIX: Compare against the cached 'oldMissing' list!
                if (stillMissing.isEmpty() && oldMissing.isNotEmpty()) {
                    onEvent(PermissionSliderEvent.SetupFinished)
                }
            }

            is PermissionSliderEvent.SetupFinished -> {
                Timber.i("Required OS permissions granted, routing to target feature")

                // 🚀 Mark as shown (in case they granted it successfully)
                viewModelScope.launch { sessionManager.setHasShownInitialNotifPrompt(true) }

                analytics.logPermissionSetupFinished(state.value.targetFeatureRoute)
                sendEffect(NavigateToFeature(state.value.targetFeatureRoute))
            }

            is PermissionSliderEvent.SkipClicked -> {
                Timber.d("User skipped permission: %s", event.permission.name)

                // Keep your existing notification flag logic if they skip notifications specifically
                if (event.permission == PermissionType.NOTIFICATIONS) {
                    viewModelScope.launch { sessionManager.setHasShownInitialNotifPrompt(true) }
                }

                // Remove the skipped permission from the list
                val stillMissing = state.value.missingPermissions.filter { it != event.permission }
                updateState { copy(missingPermissions = stillMissing) }

                // If there are no permissions left to ask for, route to the target feature
                if (stillMissing.isEmpty()) {
                    onEvent(PermissionSliderEvent.SetupFinished)
                }
            }
        }
    }
}