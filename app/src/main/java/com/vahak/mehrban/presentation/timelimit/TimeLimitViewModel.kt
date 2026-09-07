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

data class TimeLimitState(
	val isTimeLimitActive: Boolean = true,
	val hours: Int = 3,
	val minutes: Int = 0,
	val isExerciseRewardEnabled: Boolean = true,
	val maxRewardMinutes: Int = 60,
	val rewardSecondsPerPoint: Int = 30,
	val earnedBonusSecondsToday: Int = 0,
	val isWarningEnabled: Boolean = true,
	val isWeekendSeparate: Boolean = false,
	val isSaving: Boolean = false,
	val isPickerVisible: Boolean = false,
	val isMaxRewardPickerVisible: Boolean = false,
) {
	val totalMinutes: Int get() = (hours * 60) + minutes
}

sealed class TimeLimitEvent {
	data class ToggleActive(val isActive: Boolean) : TimeLimitEvent()
	data class ToggleWarning(val isActive: Boolean) : TimeLimitEvent()
	data class ToggleWeekend(val isActive: Boolean) : TimeLimitEvent()
	data class TimePresetSelected(val hours: Int, val minutes: Int) : TimeLimitEvent()

	object OpenPicker : TimeLimitEvent()
	object ClosePicker : TimeLimitEvent()
	data class ConfirmTime(val hours: Int, val minutes: Int) : TimeLimitEvent()

	object SaveClicked : TimeLimitEvent()
	object BackClicked : TimeLimitEvent()

	data class ToggleExerciseReward(val isActive: Boolean) : TimeLimitEvent()
	data class MaxRewardSelected(val hours: Int, val minutes: Int) : TimeLimitEvent()
	data class RewardSecondsChanged(val seconds: Int) : TimeLimitEvent()
	object OpenMaxRewardPicker : TimeLimitEvent()
	object CloseMaxRewardPicker : TimeLimitEvent()
}

sealed class TimeLimitEffect {
	object NavigateBack : TimeLimitEffect()
	object ShowSavedToast : TimeLimitEffect()
}

@HiltViewModel
class TimeLimitViewModel @Inject constructor(
	private val sessionManager: SessionManager,
	private val settingsRepository: SettingsRepository,
	private val analytics: AppAnalytics,
) : BaseViewModel<TimeLimitState, TimeLimitEvent, TimeLimitEffect>(TimeLimitState()) {

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
						updateState {
							copy(
								isTimeLimitActive = settings.isTimeLimitActive,
								hours = settings.dailyTimeLimitMins / 60,
								minutes = settings.dailyTimeLimitMins % 60,
								isExerciseRewardEnabled = settings.isExerciseRewardEnabled,
								maxRewardMinutes = settings.maxRewardSecondsPerDay / 60,
								rewardSecondsPerPoint = settings.rewardSecondsPerPoint.coerceIn(1, 10),
								earnedBonusSecondsToday = settings.earnedBonusSecondsToday,
							)
						}
					}
				}
			}
		}
	}

	override fun onEvent(event: TimeLimitEvent) {
		when (event) {
			is TimeLimitEvent.ToggleActive -> updateState { copy(isTimeLimitActive = event.isActive) }
			is TimeLimitEvent.ToggleWarning -> updateState { copy(isWarningEnabled = event.isActive) }
			is TimeLimitEvent.ToggleWeekend -> updateState { copy(isWeekendSeparate = event.isActive) }
			is TimeLimitEvent.TimePresetSelected -> updateState {
				copy(hours = event.hours, minutes = event.minutes)
			}

			is TimeLimitEvent.OpenPicker -> updateState { copy(isPickerVisible = true) }
			is TimeLimitEvent.ClosePicker -> updateState { copy(isPickerVisible = false) }
			is TimeLimitEvent.ConfirmTime -> updateState {
				copy(hours = event.hours, minutes = event.minutes, isPickerVisible = false)
			}

			is TimeLimitEvent.ToggleExerciseReward -> updateState { copy(isExerciseRewardEnabled = event.isActive) }
			is TimeLimitEvent.SaveClicked -> saveSettings()
			is TimeLimitEvent.BackClicked -> sendEffect(TimeLimitEffect.NavigateBack)
			is TimeLimitEvent.MaxRewardSelected -> updateState { copy(maxRewardMinutes = (event.hours * 60) + event.minutes) }
			is TimeLimitEvent.OpenMaxRewardPicker -> updateState { copy(isMaxRewardPickerVisible = true) }
			is TimeLimitEvent.CloseMaxRewardPicker -> updateState { copy(isMaxRewardPickerVisible = false) }
			is TimeLimitEvent.RewardSecondsChanged -> updateState { copy(rewardSecondsPerPoint = event.seconds) }
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
				maxRewardSeconds = state.value.maxRewardMinutes * 60,
				rewardSecondsPerPoint = state.value.rewardSecondsPerPoint
			)
			if (state.value.isTimeLimitActive) {
				analytics.logTimeLimitSet(
					totalMinutes = state.value.totalMinutes,
					isWarningEnabled = state.value.isWarningEnabled
				)
			}

			delay(500.milliseconds)
			updateState { copy(isSaving = false) }
			sendEffect(TimeLimitEffect.ShowSavedToast)
			sendEffect(TimeLimitEffect.NavigateBack)
		}
	}
}