package com.vahak.parentcontroll.presentation.report

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.dao.UsageDao
import com.vahak.parentcontroll.core.util.AppManager
import com.vahak.parentcontroll.domain.repository.ChildRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AppUsageUi(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val usedSeconds: Int
)

data class UsageReportState(
    val childName: String = "در حال بارگذاری...", // NEW: Child Name
    val totalSecondsToday: Int = 0,
    val appUsages: List<AppUsageUi> = emptyList(),
    val isLoading: Boolean = true
)

sealed class UsageReportEvent {
    object BackClicked : UsageReportEvent()
}

sealed class UsageReportEffect {
    object NavigateBack : UsageReportEffect()
}

@HiltViewModel
class UsageReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val usageDao: UsageDao,
    private val childRepository: ChildRepository, // INJECT REPOSITORY
    @ApplicationContext private val context: Context
) : BaseViewModel<UsageReportState, UsageReportEvent, UsageReportEffect>(UsageReportState()) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        loadChildInfo()
        loadDailyUsage()
    }

    private fun loadChildInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            // Using the Flow version
            childRepository.observeChildById(childId).collectLatest { child ->
                updateState { copy(childName = child?.name ?: "فرزند شما") }
            }
        }
    }

    private fun loadDailyUsage() {
        val today = LocalDate.now()

        // 1. Observe Total Time
        viewModelScope.launch(Dispatchers.IO) {
            usageDao.observeDailyUsage(childId, today).collectLatest { daily ->
                updateState { copy(totalSecondsToday = daily?.usedSeconds ?: 0) }
            }
        }

        // 2. Observe Per-App Time and attach Icons
        viewModelScope.launch(Dispatchers.IO) {
            usageDao.observeAppUsageForDay(childId, today).collectLatest { records ->
                val installedApps = AppManager.getInstalledApps(context)

                val uiList = records.map { record ->
                    val appInfo = installedApps.find { it.packageName == record.packageName }

                    AppUsageUi(
                        packageName = record.packageName,
                        appName = appInfo?.name ?: record.packageName,
                        icon = appInfo?.icon,
                        usedSeconds = record.usedSeconds
                    )
                }

                updateState { copy(appUsages = uiList, isLoading = false) }
            }
        }
    }

    override fun onEvent(event: UsageReportEvent) {
        if (event is UsageReportEvent.BackClicked) {
            sendEffect(UsageReportEffect.NavigateBack)
        }
    }
}