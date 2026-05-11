package com.vahak.parentcontroll.presentation.appselection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.dao.AppRuleDao
import com.vahak.parentcontroll.core.data.local.entity.AppRuleEntity
import com.vahak.parentcontroll.core.util.AppFetchManager
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppFilterTab { BLOCKED, ALLOWED, ALL }

data class AppSelectionStateV2(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedTab: AppFilterTab = AppFilterTab.ALL,
    val installedApps: List<AppItemUi> = emptyList()
) {
    // Helper to get the filtered list for the UI
    val filteredApps: List<AppItemUi>
        get() {
            var list = installedApps
            
            // 1. Apply Tab Filter
            list = when (selectedTab) {
                AppFilterTab.BLOCKED -> list.filter { !it.isAllowed }
                AppFilterTab.ALLOWED -> list.filter { it.isAllowed }
                AppFilterTab.ALL -> list
            }
            
            // 2. Apply Search Query Filter
            if (searchQuery.isNotBlank()) {
                list = list.filter { it.appName.contains(searchQuery, ignoreCase = true) }
            }
            
            return list
        }
}

sealed class AppSelectionEventV2 {
    data class ToggleApp(val packageName: String, val isAllowed: Boolean) : AppSelectionEventV2()
    data class UpdateSearchQuery(val query: String) : AppSelectionEventV2()
    data class TabSelected(val tab: AppFilterTab) : AppSelectionEventV2()
    object BackClicked : AppSelectionEventV2()
}

sealed class AppSelectionEffectV2 {
    object NavigateBack : AppSelectionEffectV2()
}

@HiltViewModel
class AppSelectionViewModelV2 @Inject constructor(
    private val appRuleDao: AppRuleDao,
    private val appFetchManager: AppFetchManager,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<AppSelectionStateV2, AppSelectionEventV2, AppSelectionEffectV2>(AppSelectionStateV2()) {

    private val currentChildId: String = checkNotNull(savedStateHandle["childId"])
    private var allInstalledAppsOnDevice: List<AppItemUi> = emptyList()

    init {
        loadAppsAndRules()
    }

    private fun loadAppsAndRules() {
        viewModelScope.launch(Dispatchers.IO) {
            updateState { copy(isLoading = true) }

            allInstalledAppsOnDevice = appFetchManager.getInstalledApps()

            appRuleDao.observeAllAppRules(currentChildId).collectLatest { dbRules ->
                val mergedList = allInstalledAppsOnDevice.map { app ->
                    val matchedRule = dbRules.find { it.packageName == app.packageName }
                    app.copy(isAllowed = matchedRule?.isAllowed ?: false) // Default block or allow based on your business logic
                }

                updateState {
                    copy(
                        isLoading = false,
                        installedApps = mergedList
                    )
                }
            }
        }
    }

    override fun onEvent(event: AppSelectionEventV2) {
        when (event) {
            is AppSelectionEventV2.ToggleApp -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val newRule = AppRuleEntity(
                        childId = currentChildId,
                        packageName = event.packageName,
                        isAllowed = event.isAllowed
                    )
                    appRuleDao.insertOrUpdateRule(newRule)
                }
            }
            is AppSelectionEventV2.UpdateSearchQuery -> updateState { copy(searchQuery = event.query) }
            is AppSelectionEventV2.TabSelected -> updateState { copy(selectedTab = event.tab) }
            is AppSelectionEventV2.BackClicked -> sendEffect(AppSelectionEffectV2.NavigateBack)
        }
    }
}