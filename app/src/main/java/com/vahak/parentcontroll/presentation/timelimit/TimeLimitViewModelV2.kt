// TimeLimitViewModelV2.kt
package com.vahak.parentcontroll.presentation.timelimit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimeLimitStateV2(
    val isTimeLimitActive: Boolean = true,
    val hoursInput: String = "3",
    val minutesInput: String = "0",
    val isWarningEnabled: Boolean = true,
    val isWeekendSeparate: Boolean = false,
    val isSaving: Boolean = false
) {
    val totalMinutes: Int
        get() {
            val h = hoursInput.toIntOrNull() ?: 0
            val m = minutesInput.toIntOrNull() ?: 0
            return (h * 60) + m
        }
        
    val previewText: String
        get() {
            val h = hoursInput.toIntOrNull() ?: 0
            val m = minutesInput.toIntOrNull() ?: 0
            return when {
                h > 0 && m > 0 -> "$h ساعت و $m دقیقه"
                h > 0 -> "$h ساعت"
                m > 0 -> "$m دقیقه"
                else -> "بدون محدودیت"
            }
        }
}

sealed class TimeLimitEventV2 {
    data class ToggleActive(val isActive: Boolean) : TimeLimitEventV2()
    data class ToggleWarning(val isActive: Boolean) : TimeLimitEventV2()
    data class ToggleWeekend(val isActive: Boolean) : TimeLimitEventV2()
    
    data class TimePresetSelected(val hours: Int, val minutes: Int) : TimeLimitEventV2()
    data class HoursChanged(val hours: String) : TimeLimitEventV2()
    data class MinutesChanged(val minutes: String) : TimeLimitEventV2()
    
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
    private val settingsDao: SettingsDao
) : BaseViewModel<TimeLimitStateV2, TimeLimitEventV2, TimeLimitEffectV2>(TimeLimitStateV2()) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        viewModelScope.launch {
            settingsDao.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    updateState {
                        copy(
                            isTimeLimitActive = settings.isTimeLimitActive,
                            hoursInput = (settings.dailyTimeLimitMins / 60).toString(),
                            minutesInput = (settings.dailyTimeLimitMins % 60).toString()
                            // Note: Add isWarningEnabled and isWeekendSeparate to your GlobalSettingsEntity later
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
                copy(hoursInput = event.hours.toString(), minutesInput = event.minutes.toString()) 
            }
            
            is TimeLimitEventV2.HoursChanged -> {
                val filtered = event.hours.filter { it.isDigit() }
                val h = filtered.toIntOrNull() ?: 0
                if (h <= 23) updateState { copy(hoursInput = filtered) }
            }
            
            is TimeLimitEventV2.MinutesChanged -> {
                val filtered = event.minutes.filter { it.isDigit() }
                val m = filtered.toIntOrNull() ?: 0
                if (m <= 59) updateState { copy(minutesInput = filtered) }
            }
            
            is TimeLimitEventV2.SaveClicked -> saveSettings()
            is TimeLimitEventV2.BackClicked -> sendEffect(TimeLimitEffectV2.NavigateBack)
        }
    }

    private fun saveSettings() {
        updateState { copy(isSaving = true) }
        viewModelScope.launch {
            // Save to DB
            settingsDao.updateTimeLimitToggle(childId, state.value.isTimeLimitActive)
            settingsDao.updateDailyTimeLimit(childId, state.value.totalMinutes)
            
            delay(500) // Simulated delay for smooth UX
            
            updateState { copy(isSaving = false) }
            sendEffect(TimeLimitEffectV2.ShowToast("تنظیمات زمان ذخیره شد ✅"))
            sendEffect(TimeLimitEffectV2.NavigateBack)
        }
    }
}