package com.vahak.parentcontroll.presentation.report

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.util.AppManager
import com.vahak.parentcontroll.data.remote.AppReportResponse
import com.vahak.parentcontroll.domain.repository.ChildRepository
import com.vahak.parentcontroll.domain.repository.UsageRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class AppUsageUi(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val usedSeconds: Int,
    val devices: List<DeviceUsageUi> = emptyList()
)

data class DeviceUsageUi(
    val name: String, val seconds: Int
)

data class UsageReportState(
    val activeChild: ChildEntity? = null,
    val allChildren: List<ChildEntity> = emptyList(),
    val isChildSheetOpen: Boolean = false,

    val totalSecondsToday: Int = 0,
    val weeklyUsageSeconds: List<Int> = List(7) { 0 },
    val averageSeconds: Int = 0,

    val appUsages: List<AppUsageUi> = emptyList(),
    val showAllApps: Boolean = false,

    val isLoading: Boolean = true
)

sealed class UsageReportEvent {
    object BackClicked : UsageReportEvent()
    object OpenChildSheet : UsageReportEvent()
    object CloseChildSheet : UsageReportEvent()
    data class SelectChild(val childId: String) : UsageReportEvent()
    object ToggleShowAllApps : UsageReportEvent()
}

sealed class UsageReportEffect {
    object NavigateBack : UsageReportEffect()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UsageReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionManager: SessionManager,
    private val usageRepository: UsageRepository,
    private val childRepository: ChildRepository,
    @ApplicationContext private val context: Context
) : BaseViewModel<UsageReportState, UsageReportEvent, UsageReportEffect>(UsageReportState()) {

    // Nullable so we can handle Bottom Navigation empty arguments gracefully
    private val currentChildIdFlow = MutableStateFlow(savedStateHandle.get<String>("childId"))

    // 🚀 NEW: Holds the rich network report for the Drill-Down Breakdown
    private val networkReportFlow = MutableStateFlow<AppReportResponse?>(null)

    init {
        // 1. Fetch all children for the Bottom Sheet selection
        viewModelScope.launch(Dispatchers.IO) {
            childRepository.getAllChildren().collectLatest { children ->
                updateState { copy(allChildren = children) }
            }
        }

        // 2. Fallback Logic: Resolve the active child if accessed from Bottom Navigation
        viewModelScope.launch(Dispatchers.IO) {
            if (currentChildIdFlow.value == null) {
                val activeId = sessionManager.activeChildIdFlow.firstOrNull()
                if (activeId != null) {
                    currentChildIdFlow.value = activeId
                } else {
                    val firstChild = childRepository.getAllChildren().first().firstOrNull()
                    currentChildIdFlow.value = firstChild?.id
                }
            }
        }

        // 🚀 NEW: Fetch the Drill-Down report from Spring Boot when the child changes
        viewModelScope.launch(Dispatchers.IO) {
            currentChildIdFlow.collectLatest { childId ->
                if (childId != null) {
                    val report = usageRepository.getDailyUsageReport(childId, LocalDate.now())
                    networkReportFlow.value = report
                } else {
                    networkReportFlow.value = null
                }
            }
        }

        // 3. The "Anti-Zombie" Sync Engine:
        // By using flatMapLatest, these listeners instantly cancel and switch databases
        // the millisecond a parent selects a different child from the Bottom Sheet.

        // A) Observe Active Child Profile
        viewModelScope.launch(Dispatchers.IO) {
            currentChildIdFlow.flatMapLatest { childId ->
                if (childId != null) childRepository.observeChildById(childId) else flowOf(null)
            }.collectLatest { child ->
                updateState { copy(activeChild = child) }
            }
        }

        // B) Observe Today's Total Usage (UPGRADED: Uses maxOf to ensure global accuracy)
        viewModelScope.launch(Dispatchers.IO) {
            currentChildIdFlow.flatMapLatest { childId ->
                if (childId != null) usageRepository.observeDailyUsage(
                    childId, LocalDate.now()
                ) else flowOf(null)
            }.collectLatest { daily ->
                // Ensures we show the highest known time (Local vs Offline Cache)
                val effectiveTotal = maxOf(daily?.usedSeconds ?: 0, daily?.globalUsedSeconds ?: 0)
                updateState { copy(totalSecondsToday = effectiveTotal) }
            }
        }

        // C) Observe Today's App Usages (UPGRADED: Merges Local Room DB + Spring Boot Network)
        viewModelScope.launch(Dispatchers.IO) {
            currentChildIdFlow.flatMapLatest { childId ->
                if (childId != null) {
                    combine(
                        usageRepository.observeAppUsageForDay(childId, LocalDate.now()),
                        networkReportFlow
                    ) { localRecords, networkReport ->
                        Pair(localRecords, networkReport)
                    }
                } else {
                    flowOf(Pair(emptyList(), null))
                }
            }.collectLatest { (localRecords, networkReport) ->
                val installedApps = AppManager.getInstalledApps(context)

                val uiList = if (networkReport != null) {
                    // 🌐 WE HAVE NETWORK DATA: Show global totals and device breakdowns!
                    networkReport.apps.map { networkApp ->
                        val appInfo =
                            installedApps.find { it.packageName == networkApp.packageName }
                        AppUsageUi(
                            packageName = networkApp.packageName,
                            appName = appInfo?.name ?: networkApp.packageName,
                            icon = appInfo?.icon,
                            usedSeconds = networkApp.totalSeconds,
                            devices = networkApp.devices.map {
                                DeviceUsageUi(
                                    it.deviceName, it.usedSeconds
                                )
                            })
                    }
                } else {
                    // 📴 OFFLINE MODE: Fallback to local Room data
                    localRecords.map { record ->
                        val appInfo = installedApps.find { it.packageName == record.packageName }
                        AppUsageUi(
                            packageName = record.packageName,
                            appName = appInfo?.name ?: record.packageName,
                            icon = appInfo?.icon,
                            usedSeconds = maxOf(record.usedSeconds, record.globalUsedSeconds),
                            devices = emptyList()
                        )
                    }
                }.sortedByDescending { it.usedSeconds }

                updateState { copy(appUsages = uiList, isLoading = false) }
            }
        }

        // D) Observe Weekly Chart Data (UPGRADED: Uses maxOf for historical accuracy)
        viewModelScope.launch(Dispatchers.IO) {
            currentChildIdFlow.flatMapLatest { childId ->
                if (childId == null) return@flatMapLatest flowOf(List(7) { 0 })

                val today = LocalDate.now()
                val daysToSubtract = when (today.dayOfWeek) {
                    DayOfWeek.SATURDAY -> 0
                    DayOfWeek.SUNDAY -> 1
                    DayOfWeek.MONDAY -> 2
                    DayOfWeek.TUESDAY -> 3
                    DayOfWeek.WEDNESDAY -> 4
                    DayOfWeek.THURSDAY -> 5
                    DayOfWeek.FRIDAY -> 6
                    else -> 0
                }

                val saturday = today.minusDays(daysToSubtract.toLong())
                val weekDates = (0..6).map { saturday.plusDays(it.toLong()) }

                val usageFlows = weekDates.map { date ->
                    usageRepository.observeDailyUsage(childId, date)
                }

                combine(usageFlows) { dailyUsages ->
                    dailyUsages.map { maxOf(it?.usedSeconds ?: 0, it?.globalUsedSeconds ?: 0) }
                }
            }.collectLatest { weeklySecs ->
                if (weeklySecs.isNotEmpty()) {
                    val activeDaysCount = weeklySecs.count { it > 0 }.coerceAtLeast(1)
                    val avg = weeklySecs.sum() / activeDaysCount
                    updateState {
                        copy(weeklyUsageSeconds = weeklySecs.toList(), averageSeconds = avg)
                    }
                }
            }
        }
    }

    override fun onEvent(event: UsageReportEvent) {
        when (event) {
            is UsageReportEvent.BackClicked -> sendEffect(UsageReportEffect.NavigateBack)
            is UsageReportEvent.OpenChildSheet -> updateState { copy(isChildSheetOpen = true) }
            is UsageReportEvent.CloseChildSheet -> updateState { copy(isChildSheetOpen = false) }
            is UsageReportEvent.SelectChild -> {
                updateState {
                    copy(
                        isChildSheetOpen = false,
                        isLoading = true,
                        showAllApps = false,
                        totalSecondsToday = 0,
                        weeklyUsageSeconds = List(7) { 0 },
                        averageSeconds = 0,
                        appUsages = emptyList()
                    )
                }
                currentChildIdFlow.value = event.childId
            }

            is UsageReportEvent.ToggleShowAllApps -> updateState { copy(showAllApps = !showAllApps) }
        }
    }
}