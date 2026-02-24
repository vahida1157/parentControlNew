package com.vahak.parentcontroll.presentation.timelimit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Contract
data class TimeLimitState(
    val isTimeLimitActive: Boolean = false,
    val selectedHours: Int = 1,
    val selectedMinutes: Int = 0,
    val isBottomSheetVisible: Boolean = false
)

sealed class TimeLimitEvent {
    data class ToggleActive(val isActive: Boolean) : TimeLimitEvent()
    object OpenPicker : TimeLimitEvent()
    object ClosePicker : TimeLimitEvent()
    data class ConfirmTime(val hours: Int, val minutes: Int) : TimeLimitEvent()
    object BackClicked : TimeLimitEvent()
}

sealed class TimeLimitEffect {
    object NavigateBack : TimeLimitEffect()
}

// 2. ViewModel
@HiltViewModel
class TimeLimitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsDao: SettingsDao
) : BaseViewModel<TimeLimitState, TimeLimitEvent, TimeLimitEffect>(TimeLimitState()) {

    // Fetch the ID passed from the NavGraph
    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        // Reactively listen to this child's settings in Room
        viewModelScope.launch {
            settingsDao.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    updateState {
                        copy(
                            isTimeLimitActive = settings.isTimeLimitActive,
                            selectedHours = settings.dailyTimeLimitMins / 60,
                            selectedMinutes = settings.dailyTimeLimitMins % 60
                        )
                    }
                }
            }
        }
    }

    override fun onEvent(event: TimeLimitEvent) {
        when (event) {
            is TimeLimitEvent.ToggleActive -> {
                viewModelScope.launch { settingsDao.updateTimeLimitToggle(childId, event.isActive) }
            }
            is TimeLimitEvent.OpenPicker -> updateState { copy(isBottomSheetVisible = true) }
            is TimeLimitEvent.ClosePicker -> updateState { copy(isBottomSheetVisible = false) }
            is TimeLimitEvent.ConfirmTime -> {
                val totalMinutes = (event.hours * 60) + event.minutes
                updateState { copy(isBottomSheetVisible = false) }
                viewModelScope.launch { settingsDao.updateDailyTimeLimit(childId, totalMinutes) }
            }
            is TimeLimitEvent.BackClicked -> sendEffect(TimeLimitEffect.NavigateBack)
        }
    }
}