package com.vahak.mehrban.uiv2.screens.notification

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.NotificationEntity
import com.vahak.mehrban.core.data.local.entity.NotificationType
import com.vahak.mehrban.presentation.notification.NotificationEffect
import com.vahak.mehrban.presentation.notification.NotificationEvent
import com.vahak.mehrban.presentation.notification.NotificationState
import com.vahak.mehrban.presentation.notification.NotificationViewModel
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors

@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NotificationEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    NotificationContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun NotificationContent(
    state: NotificationState,
    onEvent: (NotificationEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 🚀 Using your unified header
        MehrbanHeader(
            title = stringResource(R.string.settings_notifications),
            subtitle = stringResource(R.string.app_name),
            action = HeaderAction.Back(onClick = { onEvent(NotificationEvent.BackClicked) }),
            bottomPadding = 24.dp
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading && state.notifications.isEmpty()) {
                CircularProgressIndicator(
                    color = colors.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.notifications.isEmpty()) {
                EmptyNotificationsState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 16.dp, bottom = 40.dp, start = 20.dp, end = 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        NotificationItem(
                            notification = notification,
                            onClick = { onEvent(NotificationEvent.NotificationClicked(notification)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationEntity,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val isDark = isSystemInDarkTheme()

    // Map the type to UI colors/icons
    val (icon, tint, bg) = when (notification.type) {
        NotificationType.ALERT -> Triple(AppIcons.ShieldCheck, colors.red, colors.redLight)
        NotificationType.NEWS -> Triple(AppIcons.Info, colors.blue, colors.blue.copy(alpha = 0.15f))
        NotificationType.SYSTEM -> Triple(
            AppIcons.Smartphone,
            colors.primary,
            colors.primary.copy(alpha = 0.15f)
        )

        NotificationType.REPORT -> Triple(AppIcons.ChartBar, colors.orange, colors.orangeLight)
    }

    val timeAgo = DateUtils.getRelativeTimeSpanString(
        notification.createdAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 2.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .background(
                if (notification.isRead) colors.surface else colors.cardInnerBG,
                RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = if (!notification.isRead) colors.primary.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(bg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Unread Dot
                if (!notification.isRead) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(colors.primary, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.message,
                color = colors.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                maxLines = if (notification.isRead) 2 else 4, // Expand slightly if unread
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = timeAgo,
                color = colors.textHint,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun EmptyNotificationsState() {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(colors.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = AppIcons.Notification,
                contentDescription = null,
                tint = colors.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.notification_empty_title),
            color = colors.textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.notification_empty_description),
            color = colors.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}