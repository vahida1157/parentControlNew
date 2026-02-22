package com.vahak.parentcontroll.presentation.dashboard

import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.domain.repository.AuthRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Empty state for now since the dashboard just routes actions
object DashboardState

sealed class DashboardEvent {
    object LockClicked : DashboardEvent() // Your temporary logout
}

sealed class DashboardEffect {
    object NavigateToLogin : DashboardEffect()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<DashboardState, DashboardEvent, DashboardEffect>(DashboardState) {

    override fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.LockClicked -> performLogout()
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            authRepository.logout()
            sendEffect(DashboardEffect.NavigateToLogin)
        }
    }
}