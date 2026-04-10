package com.vahak.parentcontroll.presentation.bedtime

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
data class BedtimeState(
    val isBedtimeActive: Boolean = false,
    val startTime: LocalTime = LocalTime.of(22, 0),
    val endTime: LocalTime = LocalTime.of(7, 0),
    val currentEditMode: TimeEditMode = TimeEditMode.NONE
) {
    val isPickerVisible: Boolean get() = currentEditMode != TimeEditMode.NONE
}

sealed class BedtimeEvent {
    object BackClicked : BedtimeEvent()
    data class ToggleActive(val isActive: Boolean) : BedtimeEvent()
    data class OpenPicker(val mode: TimeEditMode) : BedtimeEvent()
    object ClosePicker : BedtimeEvent()
    data class ConfirmTime(val hours: Int, val minutes: Int) : BedtimeEvent() // Comes from UI Picker
}

sealed class BedtimeEffect {
    object NavigateBack : BedtimeEffect()
}

@HiltViewModel
class BedtimeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<BedtimeState, BedtimeEvent, BedtimeEffect>(BedtimeState()) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        // Observe DB and map it straight to our state
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    updateState {
                        copy(
                            isBedtimeActive = settings.isBedtimeActive,
                            startTime = settings.bedtimeStart,
                            endTime = settings.bedtimeEnd
                        )
                    }
                }
            }
        }
    }

    override fun onEvent(event: BedtimeEvent) {
        when (event) {
            is BedtimeEvent.BackClicked -> sendEffect(BedtimeEffect.NavigateBack)

            is BedtimeEvent.ToggleActive -> {
                viewModelScope.launch(Dispatchers.IO) {
                    settingsRepository.updateBedtimeToggle(childId, event.isActive)
                }
            }

            is BedtimeEvent.OpenPicker -> updateState { copy(currentEditMode = event.mode) }
            is BedtimeEvent.ClosePicker -> updateState { copy(currentEditMode = TimeEditMode.NONE) }

            is BedtimeEvent.ConfirmTime -> {
                val newTime = LocalTime.of(event.hours, event.minutes)
                val isStart = state.value.currentEditMode == TimeEditMode.START

                // Close picker immediately for snappy UI
                updateState { copy(currentEditMode = TimeEditMode.NONE) }

                viewModelScope.launch(Dispatchers.IO) {
                    settingsRepository.updateBedtimeSchedule(
                        childId = childId,
                        startTime = if (isStart) newTime else state.value.startTime,
                        endTime = if (!isStart) newTime else state.value.endTime
                    )
                }
            }
        }
    }
}