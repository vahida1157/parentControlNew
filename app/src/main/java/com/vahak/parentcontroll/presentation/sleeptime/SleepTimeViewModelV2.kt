package com.vahak.parentcontroll.presentation.sleeptime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.domain.repository.SettingsRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

enum class TimeEditModeV2 { NONE, START, END }

data class SleepTimeStateV2(
    val isSleepTimeActive: Boolean = false,
    val startTime: LocalTime = LocalTime.of(21, 30),
    val endTime: LocalTime = LocalTime.of(7, 0),
    val currentEditMode: TimeEditModeV2 = TimeEditModeV2.NONE,
    
    // New features from HTML
    val isDndEnabled: Boolean = true,
    val isEmergencyCallsEnabled: Boolean = true,
    val isBlueLightFilterEnabled: Boolean = false,

    val isSaving: Boolean = false
) {
    val isPickerVisible: Boolean get() = currentEditMode != TimeEditModeV2.NONE
}

sealed class SleepTimeEventV2 {
    object BackClicked : SleepTimeEventV2()
    data class ToggleActive(val isActive: Boolean) : SleepTimeEventV2()
    
    // New Toggles
    data class ToggleDnd(val isActive: Boolean) : SleepTimeEventV2()
    data class ToggleEmergencyCalls(val isActive: Boolean) : SleepTimeEventV2()
    data class ToggleBlueLight(val isActive: Boolean) : SleepTimeEventV2()

    data class OpenPicker(val mode: TimeEditModeV2) : SleepTimeEventV2()
    object ClosePicker : SleepTimeEventV2()
    data class ConfirmTime(val hours: Int, val minutes: Int) : SleepTimeEventV2()
    
    object SaveClicked : SleepTimeEventV2()
}

sealed class SleepTimeEffectV2 {
    object NavigateBack : SleepTimeEffectV2()
    data class ShowToast(val message: String) : SleepTimeEffectV2()
}

@HiltViewModel
class SleepTimeViewModelV2 @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<SleepTimeStateV2, SleepTimeEventV2, SleepTimeEffectV2>(SleepTimeStateV2()) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    updateState {
                        copy(
                            isSleepTimeActive = settings.isSleepTimeActive,
                            startTime = settings.sleepTimeStart,
                            endTime = settings.sleepTimeEnd
                            // Note: Hook up DND, Emergency, and BlueLight to DB when available
                        )
                    }
                }
            }
        }
    }

    override fun onEvent(event: SleepTimeEventV2) {
        when (event) {
            is SleepTimeEventV2.BackClicked -> sendEffect(SleepTimeEffectV2.NavigateBack)
            is SleepTimeEventV2.ToggleActive -> updateState { copy(isSleepTimeActive = event.isActive) }
            is SleepTimeEventV2.ToggleDnd -> updateState { copy(isDndEnabled = event.isActive) }
            is SleepTimeEventV2.ToggleEmergencyCalls -> updateState { copy(isEmergencyCallsEnabled = event.isActive) }
            is SleepTimeEventV2.ToggleBlueLight -> updateState { copy(isBlueLightFilterEnabled = event.isActive) }

            is SleepTimeEventV2.OpenPicker -> updateState { copy(currentEditMode = event.mode) }
            is SleepTimeEventV2.ClosePicker -> updateState { copy(currentEditMode = TimeEditModeV2.NONE) }

            is SleepTimeEventV2.ConfirmTime -> {
                val newTime = LocalTime.of(event.hours, event.minutes)
                val isStart = state.value.currentEditMode == TimeEditModeV2.START

                updateState { 
                    copy(
                        currentEditMode = TimeEditModeV2.NONE,
                        startTime = if (isStart) newTime else startTime,
                        endTime = if (!isStart) newTime else endTime
                    ) 
                }
            }

            is SleepTimeEventV2.SaveClicked -> saveSettings()
        }
    }

    private fun saveSettings() {
        updateState { copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateSleepTimeToggle(childId, state.value.isSleepTimeActive)
            settingsRepository.updateSleepTimeSchedule(
                childId = childId,
                startTime = state.value.startTime,
                endTime = state.value.endTime
            )
            
            delay(500) // Smooth UX delay
            
            updateState { copy(isSaving = false) }
            sendEffect(SleepTimeEffectV2.ShowToast("تنظیمات خواب ذخیره شد"))
            sendEffect(SleepTimeEffectV2.NavigateBack)
        }
    }
}