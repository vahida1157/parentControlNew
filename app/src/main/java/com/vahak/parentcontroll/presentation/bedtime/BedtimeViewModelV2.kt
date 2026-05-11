package com.vahak.parentcontroll.presentation.bedtime

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

data class BedtimeStateV2(
    val isBedtimeActive: Boolean = false,
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

sealed class BedtimeEventV2 {
    object BackClicked : BedtimeEventV2()
    data class ToggleActive(val isActive: Boolean) : BedtimeEventV2()
    
    // New Toggles
    data class ToggleDnd(val isActive: Boolean) : BedtimeEventV2()
    data class ToggleEmergencyCalls(val isActive: Boolean) : BedtimeEventV2()
    data class ToggleBlueLight(val isActive: Boolean) : BedtimeEventV2()

    data class OpenPicker(val mode: TimeEditModeV2) : BedtimeEventV2()
    object ClosePicker : BedtimeEventV2()
    data class ConfirmTime(val hours: Int, val minutes: Int) : BedtimeEventV2()
    
    object SaveClicked : BedtimeEventV2()
}

sealed class BedtimeEffectV2 {
    object NavigateBack : BedtimeEffectV2()
    data class ShowToast(val message: String) : BedtimeEffectV2()
}

@HiltViewModel
class BedtimeViewModelV2 @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<BedtimeStateV2, BedtimeEventV2, BedtimeEffectV2>(BedtimeStateV2()) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    updateState {
                        copy(
                            isBedtimeActive = settings.isBedtimeActive,
                            startTime = settings.bedtimeStart,
                            endTime = settings.bedtimeEnd
                            // Note: Hook up DND, Emergency, and BlueLight to DB when available
                        )
                    }
                }
            }
        }
    }

    override fun onEvent(event: BedtimeEventV2) {
        when (event) {
            is BedtimeEventV2.BackClicked -> sendEffect(BedtimeEffectV2.NavigateBack)
            is BedtimeEventV2.ToggleActive -> updateState { copy(isBedtimeActive = event.isActive) }
            is BedtimeEventV2.ToggleDnd -> updateState { copy(isDndEnabled = event.isActive) }
            is BedtimeEventV2.ToggleEmergencyCalls -> updateState { copy(isEmergencyCallsEnabled = event.isActive) }
            is BedtimeEventV2.ToggleBlueLight -> updateState { copy(isBlueLightFilterEnabled = event.isActive) }

            is BedtimeEventV2.OpenPicker -> updateState { copy(currentEditMode = event.mode) }
            is BedtimeEventV2.ClosePicker -> updateState { copy(currentEditMode = TimeEditModeV2.NONE) }

            is BedtimeEventV2.ConfirmTime -> {
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

            is BedtimeEventV2.SaveClicked -> saveSettings()
        }
    }

    private fun saveSettings() {
        updateState { copy(isSaving = true) }
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.updateBedtimeToggle(childId, state.value.isBedtimeActive)
            settingsRepository.updateBedtimeSchedule(
                childId = childId,
                startTime = state.value.startTime,
                endTime = state.value.endTime
            )
            
            delay(500) // Smooth UX delay
            
            updateState { copy(isSaving = false) }
            sendEffect(BedtimeEffectV2.ShowToast("تنظیمات خواب ذخیره شد"))
            sendEffect(BedtimeEffectV2.NavigateBack)
        }
    }
}