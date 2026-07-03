package com.vahak.mehrban.presentation.family

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.analytics.AppAnalytics
import com.vahak.mehrban.core.data.local.dao.UsageDao
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.domain.repository.ChildRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

data class FamilyChildUi(
    val child: ChildEntity, val ageYears: Int, val usageSecondsToday: Int
)

data class FamilyState(
    val children: List<FamilyChildUi> = emptyList(), val isLoading: Boolean = true
)

sealed class FamilyEvent {
    object BackClicked : FamilyEvent()
    object AddChildClicked : FamilyEvent()
    data class ChildClicked(val childId: String) : FamilyEvent()
    data class DeleteChildClicked(val childId: String) : FamilyEvent()
}

sealed class FamilyEffect {
    object NavigateBack : FamilyEffect()
    object NavigateToAddChild : FamilyEffect()
    data class NavigateToChildSettings(val childId: String) : FamilyEffect()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val usageDao: UsageDao,
    private val analytics: AppAnalytics,
) : BaseViewModel<FamilyState, FamilyEvent, FamilyEffect>(FamilyState()) {



    init {
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("Observing child profiles for family dashboard")
            childRepository.getAllChildren().flatMapLatest { children ->
                if (children.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    Timber.d(
                        "Combining child profiles with live daily usage data, profileCount: %d",
                        children.size
                    )
                    val usageFlows = children.map { child ->
                        usageDao.observeDailyUsage(child.id, LocalDate.now()).map { daily ->
                            val age = Period.between(child.dob, LocalDate.now()).years
                            FamilyChildUi(
                                child = child,
                                ageYears = age,
                                usageSecondsToday = daily?.usedSeconds ?: 0
                            )
                        }
                    }
                    combine(usageFlows) { it.toList() }
                }
            }.collectLatest { list ->
                updateState { copy(children = list, isLoading = false) }
            }
        }
    }

    override fun onEvent(event: FamilyEvent) {
        when (event) {
            is FamilyEvent.BackClicked -> sendEffect(FamilyEffect.NavigateBack)
            is FamilyEvent.AddChildClicked -> sendEffect(FamilyEffect.NavigateToAddChild)
            is FamilyEvent.ChildClicked -> sendEffect(FamilyEffect.NavigateToChildSettings(event.childId))
            is FamilyEvent.DeleteChildClicked -> {
                Timber.i("Initiating local soft-delete for child profile via Family dashboard")
                analytics.logChildDeleted()
                viewModelScope.launch(Dispatchers.IO) {
                    childRepository.deleteChildLocally(event.childId)
                }
            }
        }
    }
}