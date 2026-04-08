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

data class AppSelectionState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val installedApps: List<AppItemUi> = emptyList()
)

sealed class AppSelectionEvent {
    data class ToggleApp(val packageName: String, val isAllowed: Boolean) : AppSelectionEvent()
    data class UpdateSearchQuery(val query: String) : AppSelectionEvent()
    object BackClicked : AppSelectionEvent()
}

sealed class AppSelectionEffect {
    object NavigateBack : AppSelectionEffect()
}

@HiltViewModel
class AppSelectionViewModel @Inject constructor(
    private val appRuleDao: AppRuleDao,
    private val appFetchManager: AppFetchManager,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<AppSelectionState, AppSelectionEvent, AppSelectionEffect>(AppSelectionState()) {

    // Retrieve the childId safely from navigation
    private val currentChildId: String = checkNotNull(savedStateHandle["childId"])

    private var allInstalledAppsOnDevice: List<AppItemUi> = emptyList()

    init {
        loadAppsAndRules()
    }

    private fun loadAppsAndRules() {
        viewModelScope.launch(Dispatchers.IO) {
            updateState { copy(isLoading = true) }

            // 1. Fetch real apps from the OS
            allInstalledAppsOnDevice = appFetchManager.getInstalledApps()

            // 2. Watch your existing AppRuleDao for this child
            appRuleDao.observeAllAppRules(currentChildId).collectLatest { dbRules ->

                // 3. Merge OS apps with Database rules
                val mergedList = allInstalledAppsOnDevice.map { app ->
                    val matchedRule = dbRules.find { it.packageName == app.packageName }
                    app.copy(isAllowed = matchedRule?.isAllowed ?: false)
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

    override fun onEvent(event: AppSelectionEvent) {
        when (event) {
            is AppSelectionEvent.ToggleApp -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val newRule = AppRuleEntity(
                        childId = currentChildId,
                        packageName = event.packageName,
                        isAllowed = event.isAllowed
                    )
                    // Insert or replace using your existing DAO method
                    appRuleDao.insertOrUpdateRule(newRule)
                }
            }

            is AppSelectionEvent.UpdateSearchQuery -> {
                updateState { copy(searchQuery = event.query) }
                // Optional: If you want the search bar to actually filter the list on-screen,
                // you would apply a filter to `mergedList` up in the collectLatest block based on this query.
            }

            is AppSelectionEvent.BackClicked -> {
                sendEffect(AppSelectionEffect.NavigateBack)
            }
        }
    }
}