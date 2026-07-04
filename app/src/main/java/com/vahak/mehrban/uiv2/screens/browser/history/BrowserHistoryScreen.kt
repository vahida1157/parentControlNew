package com.vahak.mehrban.uiv2.screens.browser.history

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.presentation.browser.settings.history.BrowserHistoryEvent
import com.vahak.mehrban.presentation.browser.settings.history.BrowserHistoryState
import com.vahak.mehrban.presentation.browser.settings.history.BrowserHistoryViewModel
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import java.util.Date

@Composable
fun BrowserHistoryScreen(
    viewModel: BrowserHistoryViewModel = hiltViewModel(), onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    BrowserHistoryContent(state = state, onEvent = viewModel::onEvent, onBackClick = onBackClick)
}

@Composable
fun BrowserHistoryContent(
    state: BrowserHistoryState, onEvent: (BrowserHistoryEvent) -> Unit, onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        MehrbanHeader(
            title = stringResource(R.string.browser_history),
            subtitle = stringResource(R.string.browser_back_to_menu),
            iconEmoji = "🕒",
            action = HeaderAction.Back(onClick = onBackClick)
        )

        // Date Navigator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onEvent(BrowserHistoryEvent.ChangeDate(-1)) }) {
                Icon(
                    AppIcons.ChevronLeft,
                    contentDescription = "Previous Day",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = DateFormat.format("MMM dd, yyyy", Date(state.selectedDateMillis)).toString(),
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            IconButton(onClick = { onEvent(BrowserHistoryEvent.ChangeDate(1)) }) {
                Icon(
                    AppIcons.ChevronLeft,
                    contentDescription = "Next Day",
                    tint = colors.textPrimary,
                    modifier = Modifier.graphicsLayer { scaleX = -1f })
            }
        }

        // History List
        if (state.history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.browser_empty_list_hint), color = colors.textHint)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                items(state.history) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                item.title,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                item.url,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = DateFormat.format("HH:mm", Date(item.timestamp)).toString(),
                                fontSize = 10.sp,
                                color = colors.textHint
                            )
                        }
                    }
                }
            }
        }
    }
}