package com.vahak.mehrban.presentation.appselection

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.R
import com.vahak.mehrban.core.util.AppFetchManager
import com.vahak.mehrban.domain.repository.AppRuleRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

enum class AppFilterTab { BLOCKED, ALLOWED, ALL }

data class AppSelectionStateV2(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
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
    object SaveClicked : AppSelectionEventV2()
    object BackClicked : AppSelectionEventV2()
}

sealed class AppSelectionEffectV2 {
    object NavigateBack : AppSelectionEffectV2()
    data class ShowToast(val message: String) : AppSelectionEffectV2()
}

@HiltViewModel
class AppSelectionViewModelV2 @Inject constructor(
    private val appRuleRepository: AppRuleRepository,
    private val appFetchManager: AppFetchManager,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
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
            Timber.d("Fetching installed applications from device")
            updateState { copy(isLoading = true) }

            allInstalledAppsOnDevice = appFetchManager.getInstalledApps()

            Timber.d("Observing local application rules for merge")
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
                Timber.d(
                    "UI requested application rule toggle, packageName: %s, isAllowed: %b",
                    event.packageName,
                    event.isAllowed
                )
                viewModelScope.launch(Dispatchers.IO) {
                    appRuleRepository.toggleAppRule(
                        currentChildId, event.packageName, event.isAllowed
                    )
                }
            }

            is AppSelectionEventV2.UpdateSearchQuery -> updateState { copy(searchQuery = event.query) }
            is AppSelectionEventV2.TabSelected -> updateState { copy(selectedTab = event.tab) }

            is AppSelectionEventV2.SaveClicked -> {
                Timber.d("UI requested manual rule synchronization to server")
                viewModelScope.launch(Dispatchers.IO) {
                    updateState { copy(isSaving = true) }

                    val result = appRuleRepository.pushRulesToServer(currentChildId)

                    withContext(Dispatchers.Main) {
                        updateState { copy(isSaving = false) }
                        if (result.isSuccess) {
                            Timber.i("Application rules manually pushed to server successfully")
                            sendEffect(AppSelectionEffectV2.ShowToast(context.getString(R.string.app_access_saved)))
                            sendEffect(AppSelectionEffectV2.NavigateBack)
                        } else {
                            // Using warn because offline-first architecture will auto-retry later
                            Timber.w("Manual rule push failed, retaining local dirty state")
                            sendEffect(AppSelectionEffectV2.ShowToast(context.getString(R.string.error_server_communication)))
                        }
                    }
                }
            }

            is AppSelectionEventV2.BackClicked -> sendEffect(AppSelectionEffectV2.NavigateBack)
        }
    }
}