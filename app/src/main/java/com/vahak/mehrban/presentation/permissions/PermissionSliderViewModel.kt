package com.vahak.mehrban.presentation.permissions

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.vahak.mehrban.core.analytics.AppAnalytics
import com.vahak.mehrban.core.util.PermissionChecker
import com.vahak.mehrban.core.util.PermissionType
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

data class PermissionSliderState(
    val missingPermissions: List<PermissionType> = emptyList(), val targetFeatureRoute: String = ""
)

sealed class PermissionSliderEvent {
    data class GrantClicked(val permission: PermissionType) : PermissionSliderEvent()
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
                    PermissionSliderEffect.LaunchAndroidSettings(
                        action = event.permission.androidSettingsAction,
                        permission = event.permission
                    )
                )
            }
            is PermissionSliderEvent.CheckPermissions -> {
                // 1. Find out which ones are STILL missing
                val stillMissing = state.value.missingPermissions.filter {
                    !PermissionChecker.hasPermission(event.context, it)
                }

                // 2. Find out which ones were just granted (Old list MINUS New list)
                val newlyGranted = state.value.missingPermissions - stillMissing.toSet()

                // 3. Log success for each one they figured out!
                newlyGranted.forEach { permission ->
                    Timber.d("Detected OS permission newly granted: %s", permission.name)

                    // 🚀 FIRE EVENT: True success tracked!
                    analytics.logPermissionStepSuccess(permission.name)
                }

                // 4. Update the UI state so the slider moves forward
                updateState { copy(missingPermissions = stillMissing) }
            }

            is PermissionSliderEvent.SetupFinished -> {
                Timber.i("Required OS permissions granted, routing to target feature")
                analytics.logPermissionSetupFinished(state.value.targetFeatureRoute)
                sendEffect(PermissionSliderEffect.NavigateToFeature(state.value.targetFeatureRoute))
            }
        }
    }
}