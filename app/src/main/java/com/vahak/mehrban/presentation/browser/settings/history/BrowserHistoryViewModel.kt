package com.vahak.mehrban.presentation.browser.settings.history

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.BrowserHistoryEntity
import com.vahak.mehrban.domain.repository.SafeBrowserRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class BrowserHistoryState(
    val childId: String = "",
    val history: List<BrowserHistoryEntity> = emptyList(),
    val selectedDateMillis: Long = System.currentTimeMillis()
)

sealed class BrowserHistoryEvent {
    data class ChangeDate(val offsetDays: Int) : BrowserHistoryEvent()
}

sealed class BrowserHistoryEffect

@HiltViewModel
class BrowserHistoryViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: SafeBrowserRepository
) : BaseViewModel<BrowserHistoryState, BrowserHistoryEvent, BrowserHistoryEffect>(BrowserHistoryState()) {

    private var historyJob: Job? = null

    init {
        viewModelScope.launch {
            sessionManager.viewedChildIdFlow.collectLatest { childId ->
                if (childId != null) {
                    updateState { copy(childId = childId) }
                    observeHistory(childId, state.value.selectedDateMillis)
                }
            }
        }
    }

    private fun observeHistory(childId: String, dateMillis: Long) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            repository.observeHistoryForDate(childId, startOfDay, endOfDay)
                .collectLatest { historyList ->
                    updateState { copy(history = historyList) }
                }
        }
    }

    override fun onEvent(event: BrowserHistoryEvent) {
        when (event) {
            is BrowserHistoryEvent.ChangeDate -> {
                val cal = Calendar.getInstance().apply { timeInMillis = state.value.selectedDateMillis }
                cal.add(Calendar.DAY_OF_YEAR, event.offsetDays)
                val newDate = cal.timeInMillis
                updateState { copy(selectedDateMillis = newDate) }
                observeHistory(state.value.childId, newDate)
            }
        }
    }
}