package com.vahak.parentcontroll.presentation.timelimit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.domain.repository.SettingsRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isPickerVisible: Boolean = false
) {
    val totalMinutes: Int get() = (hours * 60) + minutes

    val previewText: String
        get() = when {
            hours > 0 && minutes > 0 -> "$hours ساعت و $minutes دقیقه"
            hours > 0 -> "$hours ساعت"
            minutes > 0 -> "$minutes دقیقه"
            else -> "بدون محدودیت"
        }
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
    private val settingsRepository: SettingsRepository // CHANGED: Replaced DAO with Repository
) : BaseViewModel<TimeLimitStateV2, TimeLimitEventV2, TimeLimitEffectV2>(TimeLimitStateV2()) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        // 1. Fire a background sync when the screen opens to get any changes from other devices
        viewModelScope.launch {
            settingsRepository.syncSettingsFromServer(childId)
        }

        // 2. Observe the local database via the Repository
        viewModelScope.launch {
            settingsRepository.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    updateState {
                        copy(
                            isTimeLimitActive = settings.isTimeLimitActive,
                            hours = settings.dailyTimeLimitMins / 60,
                            minutes = settings.dailyTimeLimitMins % 60
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
            is TimeLimitEventV2.TimePresetSelected -> updateState {
                copy(
                    hours = event.hours,
                    minutes = event.minutes
                )
            }

            is TimeLimitEventV2.OpenPicker -> updateState { copy(isPickerVisible = true) }
            is TimeLimitEventV2.ClosePicker -> updateState { copy(isPickerVisible = false) }
            is TimeLimitEventV2.ConfirmTime -> updateState {
                copy(
                    hours = event.hours,
                    minutes = event.minutes,
                    isPickerVisible = false
                )
            }

            is TimeLimitEventV2.SaveClicked -> saveSettings()
            is TimeLimitEventV2.BackClicked -> sendEffect(TimeLimitEffectV2.NavigateBack)
        }
    }

    private fun saveSettings() {
        updateState { copy(isSaving = true) }
        viewModelScope.launch {
            // This now saves to Room AND automatically pushes the JSON payload to Spring Boot!
            settingsRepository.updateTimeLimit(
                childId = childId,
                isActive = state.value.isTimeLimitActive,
                limitMins = state.value.totalMinutes
            )

            delay(500)
            updateState { copy(isSaving = false) }
            sendEffect(TimeLimitEffectV2.ShowToast("تنظیمات زمان ذخیره شد ✅"))
            sendEffect(TimeLimitEffectV2.NavigateBack)
        }
    }
}