package com.vahak.parentcontroll.presentation.appselection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.util.AppFetchManager
import com.vahak.parentcontroll.domain.repository.AppRuleRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class AppFilterTab { BLOCKED, ALLOWED, ALL }

data class AppSelectionStateV2(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false, // NEW: Track save state
    val searchQuery: String = "",
    val selectedTab: AppFilterTab = AppFilterTab.ALL,
    val installedApps: List<AppItemUi> = emptyList()
) {
    val filteredApps: List<AppItemUi>
        get() {
            var list = installedApps
            list = when (selectedTab) {
                AppFilterTab.BLOCKED -> list.filter { !it.isAllowed }
                AppFilterTab.ALLOWED -> list.filter { it.isAllowed }
                AppFilterTab.ALL -> list
            }
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
    object SaveClicked : AppSelectionEventV2() // NEW: Explicit Save Event
    object BackClicked : AppSelectionEventV2()
}

sealed class AppSelectionEffectV2 {
    object NavigateBack : AppSelectionEffectV2()
    data class ShowToast(val message: String) : AppSelectionEffectV2() // NEW: Toast effect
}

@HiltViewModel
class AppSelectionViewModelV2 @Inject constructor(
    private val appRuleRepository: AppRuleRepository,
    private val appFetchManager: AppFetchManager,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<AppSelectionStateV2, AppSelectionEventV2, AppSelectionEffectV2>(
    AppSelectionStateV2()
) {

    private val currentChildId: String = checkNotNull(savedStateHandle["childId"])
    private var allInstalledAppsOnDevice: List<AppItemUi> = emptyList()

    init {
        loadAppsAndRules()
    }

    private fun loadAppsAndRules() {
        viewModelScope.launch(Dispatchers.IO) {
            updateState { copy(isLoading = true) }

            allInstalledAppsOnDevice = appFetchManager.getInstalledApps()
            appRuleRepository.syncRulesFromServer(currentChildId)

            appRuleRepository.observeAllRules(currentChildId).collectLatest { dbRules ->
                val dbRulesMap = dbRules.associateBy { it.packageName }
                val mergedList = allInstalledAppsOnDevice.map { app ->
                    val matchedRule = dbRulesMap[app.packageName]
                    app.copy(isAllowed = matchedRule?.isAllowed ?: false)
                }

                withContext(Dispatchers.Main) {
                    updateState { copy(isLoading = false, installedApps = mergedList) }
                }
            }
        }
    }

    override fun onEvent(event: AppSelectionEventV2) {
        when (event) {
            is AppSelectionEventV2.ToggleApp -> {
                viewModelScope.launch(Dispatchers.IO) {
                    appRuleRepository.toggleAppRule(
                        currentChildId,
                        event.packageName,
                        event.isAllowed
                    )
                }
            }

            is AppSelectionEventV2.UpdateSearchQuery -> updateState { copy(searchQuery = event.query) }
            is AppSelectionEventV2.TabSelected -> updateState { copy(selectedTab = event.tab) }

            is AppSelectionEventV2.SaveClicked -> {
                // FIXED: Explicitly push only when the user wants to save
                viewModelScope.launch(Dispatchers.IO) {
                    updateState { copy(isSaving = true) }
                    appRuleRepository.pushRulesToServer(currentChildId)

                    withContext(Dispatchers.Main) {
                        updateState { copy(isSaving = false) }
                        sendEffect(AppSelectionEffectV2.ShowToast("دسترسی برنامه‌ها ذخیره شد ✅"))
                        sendEffect(AppSelectionEffectV2.NavigateBack)
                    }
                }
            }

            is AppSelectionEventV2.BackClicked -> {
                // FIXED: Back button just goes back. It no longer auto-saves!
                sendEffect(AppSelectionEffectV2.NavigateBack)
            }
        }
    }
}