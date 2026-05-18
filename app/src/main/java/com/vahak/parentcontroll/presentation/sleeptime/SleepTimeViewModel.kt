package com.vahak.parentcontroll.presentation.sleeptime

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.domain.repository.SettingsRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

enum class TimeEditMode { NONE, START, END }

// Using LocalTime natively in our State!
data class SleepTimeState(
    val isSleepTimeActive: Boolean = false,
    val startTime: LocalTime = LocalTime.of(22, 0),
    val endTime: LocalTime = LocalTime.of(7, 0),
    val currentEditMode: TimeEditMode = TimeEditMode.NONE
) {
    val isPickerVisible: Boolean get() = currentEditMode != TimeEditMode.NONE
}

sealed class SleepTimeEvent {
    object BackClicked : SleepTimeEvent()
    data class ToggleActive(val isActive: Boolean) : SleepTimeEvent()
    data class OpenPicker(val mode: TimeEditMode) : SleepTimeEvent()
    object ClosePicker : SleepTimeEvent()
    data class ConfirmTime(val hours: Int, val minutes: Int) : SleepTimeEvent() // Comes from UI Picker
}

sealed class SleepTimeEffect {
    object NavigateBack : SleepTimeEffect()
}

@HiltViewModel
class SleepTimeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<SleepTimeState, SleepTimeEvent, SleepTimeEffect>(SleepTimeState()) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        // Observe DB and map it straight to our state
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    updateState {
                        copy(
                            isSleepTimeActive = settings.isSleepTimeActive,
                            startTime = settings.sleepTimeStart,
                            endTime = settings.sleepTimeEnd
                        )
                    }
                }
            }
        }
    }

    override fun onEvent(event: SleepTimeEvent) {
        when (event) {
            is SleepTimeEvent.BackClicked -> sendEffect(SleepTimeEffect.NavigateBack)

            is SleepTimeEvent.ToggleActive -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsRepository.updateSleepTimeToggle(childId, event.isActive)
                }
            }

            is SleepTimeEvent.OpenPicker -> updateState { copy(currentEditMode = event.mode) }
            is SleepTimeEvent.ClosePicker -> updateState { copy(currentEditMode = TimeEditMode.NONE) }

            is SleepTimeEvent.ConfirmTime -> {
                val newTime = LocalTime.of(event.hours, event.minutes)
                val isStart = state.value.currentEditMode == TimeEditMode.START

                // Close picker immediately for snappy UI
                updateState { copy(currentEditMode = TimeEditMode.NONE) }

                viewModelScope.launch(Dispatchers.IO) {
                    settingsRepository.updateSleepTimeSchedule(
                        childId = childId,
                        startTime = if (isStart) newTime else state.value.startTime,
                        endTime = if (!isStart) newTime else state.value.endTime
                    )
                }
            }
        }
    }
}