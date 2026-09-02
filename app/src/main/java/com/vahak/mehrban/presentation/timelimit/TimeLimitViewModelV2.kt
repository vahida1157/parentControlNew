package com.vahak.mehrban.presentation.timelimit

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.analytics.AppAnalytics
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.domain.repository.SettingsRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

data class TimeLimitStateV2(
    val isTimeLimitActive: Boolean = true,
    val hours: Int = 3,
    val minutes: Int = 0,
    val isExerciseRewardEnabled: Boolean = true,
    val maxRewardHours: Int = 2,
    val isWarningEnabled: Boolean = true,
    val isWeekendSeparate: Boolean = false,
    val isSaving: Boolean = false,
    val isPickerVisible: Boolean = false,
) {
    val totalMinutes: Int get() = (hours * 60) + minutes
}

sealed class TimeLimitEventV2 {
    data class ToggleActive(val isActive: Boolean) : TimeLimitEventV2()
    data class ToggleWarning(val isActive: Boolean) : TimeLimitEventV2()
    data class ToggleWeekend(val isActive: Boolean) : TimeLimitEventV2()
    data class TimePresetSelected(val hours: Int, val minutes: Int) : TimeLimitEventV2()

    object OpenPicker : TimeLimitEventV2()
    object ClosePicker : TimeLimitEventV2()
    data class ConfirmTime(val hours: Int, val minutes: Int) : TimeLimitEventV2()

    object SaveClicked : TimeLimitEventV2()
    object BackClicked : TimeLimitEventV2()

    data class ToggleExerciseReward(val isActive: Boolean) : TimeLimitEventV2()
    data class MaxRewardSelected(val hours: Int) : TimeLimitEventV2()
}

sealed class TimeLimitEffectV2 {
    object NavigateBack : TimeLimitEffectV2()
    object ShowSavedToast : TimeLimitEffectV2()
}

@HiltViewModel
class TimeLimitViewModelV2 @Inject constructor(
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
    private val analytics: AppAnalytics,
) : BaseViewModel<TimeLimitStateV2, TimeLimitEventV2, TimeLimitEffectV2>(TimeLimitStateV2()) {

    private var currentChildId: String? = null

    init {
        viewModelScope.launch {
            val childId = sessionManager.viewedChildIdFlow.firstOrNull()
                ?: sessionManager.activeChildIdFlow.firstOrNull()

            if (childId != null) {
                currentChildId = childId
                Timber.d("Observing daily application time limit configuration")
                settingsRepository.getGlobalSettings(childId).collectLatest { settings ->
                    if (settings != null) {
                        val h = settings.dailyTimeLimitMins / 60
                        val m = settings.dailyTimeLimitMins % 60
                        updateState {
                            copy(
                                isTimeLimitActive = settings.isTimeLimitActive,
                                hours = h,
                                minutes = m,
                                isExerciseRewardEnabled = settings.isExerciseRewardEnabled,
                                maxRewardHours = settings.maxRewardSecondsPerDay / 3600,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onEvent(event: TimeLimitEventV2) {
        when (event) {
            is TimeLimitEventV2.ToggleActive -> updateState { copy(isTimeLimitActive = event.isActive) }
            is TimeLimitEventV2.ToggleWarning -> updateState { copy(isWarningEnabled = event.isActive) }
            is TimeLimitEventV2.ToggleWeekend -> updateState { copy(isWeekendSeparate = event.isActive) }
            is TimeLimitEventV2.TimePresetSelected -> updateState {
                copy(hours = event.hours, minutes = event.minutes)
            }

            is TimeLimitEventV2.OpenPicker -> updateState { copy(isPickerVisible = true) }
            is TimeLimitEventV2.ClosePicker -> updateState { copy(isPickerVisible = false) }
            is TimeLimitEventV2.ConfirmTime -> updateState {
                copy(hours = event.hours, minutes = event.minutes, isPickerVisible = false)
            }

            is TimeLimitEventV2.ToggleExerciseReward -> updateState { copy(isExerciseRewardEnabled = event.isActive) }
            is TimeLimitEventV2.MaxRewardSelected -> updateState { copy(maxRewardHours = event.hours) }
            is TimeLimitEventV2.SaveClicked -> saveSettings()
            is TimeLimitEventV2.BackClicked -> sendEffect(TimeLimitEffectV2.NavigateBack)
        }
    }

    private fun saveSettings() {
        val childId = currentChildId ?: return

        Timber.i("Persisting daily application time limit configuration locally")
        updateState { copy(isSaving = true) }
        viewModelScope.launch {
            settingsRepository.updateTimeLimit(
                childId = childId,
                isActive = state.value.isTimeLimitActive,
                limitMins = state.value.totalMinutes,
                isRewardEnabled = state.value.isExerciseRewardEnabled,
                maxRewardHours = state.value.maxRewardHours
            )
            if (state.value.isTimeLimitActive) {
                analytics.logTimeLimitSet(
                    totalMinutes = state.value.totalMinutes,
                    isWarningEnabled = state.value.isWarningEnabled
                )
            }

            delay(500.milliseconds)
            updateState { copy(isSaving = false) }
            sendEffect(TimeLimitEffectV2.ShowSavedToast)
            sendEffect(TimeLimitEffectV2.NavigateBack)
        }
    }
}