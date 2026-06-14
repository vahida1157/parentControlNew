package com.vahak.mehrban.presentation.timelimit

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.R
import com.vahak.mehrban.domain.repository.SettingsRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimeLimitStateV2(
    val isTimeLimitActive: Boolean = true,
    val hours: Int = 3,
    val minutes: Int = 0,
    val isWarningEnabled: Boolean = true,
    val isWeekendSeparate: Boolean = false,
    val isSaving: Boolean = false,
    val isPickerVisible: Boolean = false,
    val previewText: String = ""
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
}

sealed class TimeLimitEffectV2 {
    object NavigateBack : TimeLimitEffectV2()
    data class ShowToast(val message: String) : TimeLimitEffectV2()
}

@HiltViewModel
class TimeLimitViewModelV2 @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : BaseViewModel<TimeLimitStateV2, TimeLimitEventV2, TimeLimitEffectV2>(TimeLimitStateV2()) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        // Provide initial localized preview text
        updateState { copy(previewText = buildPreviewText(hours, minutes)) }

        viewModelScope.launch {
            settingsRepository.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    val h = settings.dailyTimeLimitMins / 60
                    val m = settings.dailyTimeLimitMins % 60
                    updateState {
                        copy(
                            isTimeLimitActive = settings.isTimeLimitActive,
                            hours = h,
                            minutes = m,
                            previewText = buildPreviewText(h, m)
                        )
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
            is TimeLimitEventV2.TimePresetSelected -> {
                val h = event.hours
                val m = event.minutes
                updateState { copy(hours = h, minutes = m, previewText = buildPreviewText(h, m)) }
            }

            is TimeLimitEventV2.OpenPicker -> updateState { copy(isPickerVisible = true) }
            is TimeLimitEventV2.ClosePicker -> updateState { copy(isPickerVisible = false) }
            is TimeLimitEventV2.ConfirmTime -> {
                val h = event.hours
                val m = event.minutes
                updateState {
                    copy(
                        hours = h,
                        minutes = m,
                        isPickerVisible = false,
                        previewText = buildPreviewText(h, m)
                    )
                }
            }

            is TimeLimitEventV2.SaveClicked -> saveSettings()
            is TimeLimitEventV2.BackClicked -> sendEffect(TimeLimitEffectV2.NavigateBack)
        }
    }

    private fun saveSettings() {
        updateState { copy(isSaving = true) }
        viewModelScope.launch {
            settingsRepository.updateTimeLimit(
                childId = childId,
                isActive = state.value.isTimeLimitActive,
                limitMins = state.value.totalMinutes
            )

            delay(500)
            updateState { copy(isSaving = false) }
            sendEffect(TimeLimitEffectV2.ShowToast(context.getString(R.string.time_settings_saved)))
            sendEffect(TimeLimitEffectV2.NavigateBack)
        }
    }

    private fun buildPreviewText(hours: Int, minutes: Int): String {
        val hourLabel = context.getString(R.string.hour)
        val minuteLabel = context.getString(R.string.minute)
        val noLimit = context.getString(R.string.unlimited)

        return when {
            hours > 0 && minutes > 0 -> "$hours $hourLabel و $minutes $minuteLabel"
            hours > 0 -> "$hours $hourLabel"
            minutes > 0 -> "$minutes $minuteLabel"
            else -> noLimit
        }
    }
}