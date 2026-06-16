package com.vahak.mehrban.presentation.permissions

import androidx.lifecycle.SavedStateHandle
import com.vahak.mehrban.core.util.PermissionType
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

// 1. Contract
data class PermissionSliderState(
    val missingPermissions: List<PermissionType> = emptyList(), val targetFeatureRoute: String = ""
)

sealed class PermissionSliderEvent {
    data class GrantClicked(val permission: PermissionType) : PermissionSliderEvent()
    object SetupFinished : PermissionSliderEvent()
}

sealed class PermissionSliderEffect {
    // Sends the action string (e.g., Settings.ACTION_USAGE_ACCESS_SETTINGS) to the UI
    data class LaunchAndroidSettings(val action: String, val permission: PermissionType) :
        PermissionSliderEffect()

    data class NavigateToFeature(val route: String) : PermissionSliderEffect()
}

// 2. ViewModel
@HiltViewModel
class PermissionSliderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
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

        updateState {
            copy(targetFeatureRoute = route, missingPermissions = permissionsList)
        }
    }

    override fun onEvent(event: PermissionSliderEvent) {
        when (event) {
            is PermissionSliderEvent.GrantClicked -> {
                Timber.d("User initiated OS permission request: %s", event.permission)
                sendEffect(
                    PermissionSliderEffect.LaunchAndroidSettings(
                        action = event.permission.androidSettingsAction,
                        permission = event.permission
                    )
                )
            }

            is PermissionSliderEvent.SetupFinished -> {
                Timber.i("Required OS permissions granted, routing to target feature")
                sendEffect(PermissionSliderEffect.NavigateToFeature(state.value.targetFeatureRoute))
            }
        }
    }
}