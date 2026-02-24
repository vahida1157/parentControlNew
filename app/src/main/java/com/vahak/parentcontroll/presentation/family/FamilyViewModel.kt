package com.vahak.parentcontroll.presentation.family

import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.domain.repository.ChildRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FamilyState(
    val children: List<ChildEntity> = emptyList(),
    val isLoading: Boolean = true
)

sealed class FamilyEvent {
    object BackClicked : FamilyEvent()
    object AddChildClicked : FamilyEvent()
    data class ChildClicked(val childId: String) : FamilyEvent()
}

sealed class FamilyEffect {
    object NavigateBack : FamilyEffect()
    object NavigateToAddChild : FamilyEffect()
    data class NavigateToChildSettings(val childId: String) : FamilyEffect()
}

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val childRepository: ChildRepository
) : BaseViewModel<FamilyState, FamilyEvent, FamilyEffect>(FamilyState()) {

    init {
        // Automatically listen to DB changes
        viewModelScope.launch {
            childRepository.getAllChildren().collectLatest { childList ->
                updateState { copy(children = childList, isLoading = false) }
            }
        }
    }

    override fun onEvent(event: FamilyEvent) {
        when (event) {
            is FamilyEvent.BackClicked -> sendEffect(FamilyEffect.NavigateBack)
            is FamilyEvent.AddChildClicked -> sendEffect(FamilyEffect.NavigateToAddChild)
            is FamilyEvent.ChildClicked -> sendEffect(FamilyEffect.NavigateToChildSettings(event.childId))
        }
    }
}