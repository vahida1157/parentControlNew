package com.vahak.mehrban.presentation.notification

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.entity.NotificationEntity
import com.vahak.mehrban.domain.repository.NotificationRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationState(
    val notifications: List<NotificationEntity> = emptyList(),
    val isLoading: Boolean = true
)

sealed class NotificationEvent {
    object BackClicked : NotificationEvent()
    data class NotificationClicked(val notification: NotificationEntity) : NotificationEvent()
    object Refresh : NotificationEvent()
}

sealed class NotificationEffect {
    object NavigateBack : NotificationEffect()
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : BaseViewModel<NotificationState, NotificationEvent, NotificationEffect>(NotificationState()) {

    init {
        viewModelScope.launch {
            notificationRepository.observeAllNotifications().collectLatest { list ->
                updateState { copy(notifications = list, isLoading = false) }
            }
        }
        
        // Fetch new remote notifications when the screen opens
        viewModelScope.launch {
            notificationRepository.syncNotificationsFromServer()
        }
    }

    override fun onEvent(event: NotificationEvent) {
        when (event) {
            is NotificationEvent.BackClicked -> {
                viewModelScope.launch { sendEffect(NotificationEffect.NavigateBack) }
            }
            is NotificationEvent.NotificationClicked -> {
                if (!event.notification.isRead) {
                    viewModelScope.launch {
                        notificationRepository.markAsRead(event.notification.id)
                    }
                }
                // TODO: Depending on the notification type, you could emit an effect 
                // to navigate the user to a specific screen (like Child Settings)
            }
            is NotificationEvent.Refresh -> {
                updateState { copy(isLoading = true) }
                viewModelScope.launch {
                    notificationRepository.syncNotificationsFromServer()
                    updateState { copy(isLoading = false) }
                }
            }
        }
    }
}