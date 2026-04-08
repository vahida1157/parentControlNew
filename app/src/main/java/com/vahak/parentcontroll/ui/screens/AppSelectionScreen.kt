package com.vahak.parentcontroll.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.appselection.AppItemUi
import com.vahak.parentcontroll.presentation.appselection.AppSelectionEffect
import com.vahak.parentcontroll.presentation.appselection.AppSelectionEvent
import com.vahak.parentcontroll.presentation.appselection.AppSelectionState
import com.vahak.parentcontroll.presentation.appselection.AppSelectionViewModel
import com.vahak.parentcontroll.ui.component.AppToggleItem
import com.vahak.parentcontroll.ui.component.SimpleFlatHeader
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

// 1. STATEFUL WRAPPER
@Composable
fun AppSelectionScreen(
    viewModel: AppSelectionViewModel = hiltViewModel(), onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is AppSelectionEffect.NavigateBack) onBackClick()
        }
    }

    AppSelectionContent(
        state = state, onEvent = viewModel::onEvent
    )
}

// 2. STATELESS CONTENT
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionContent(
    state: AppSelectionState, onEvent: (AppSelectionEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Header
        SimpleFlatHeader(
            title = "مدیریت برنامه‌ها", onBackClick = { onEvent(AppSelectionEvent.BackClicked) })

        // Search Bar Section
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "کدام برنامه‌ها برای فرزندتان مجاز است؟",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onEvent(AppSelectionEvent.UpdateSearchQuery(it)) },
                placeholder = { Text("جستجوی برنامه...", color = colors.textHint) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.divider
                ),
                singleLine = true
            )
        }

        // App List Section
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center), color = colors.primary
                )
            } else if (state.installedApps.isEmpty()) {
                Text(
                    text = "برنامه‌ای یافت نشد.",
                    color = colors.textHint,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Quick local search filter implementation
                    val filteredApps = if (state.searchQuery.isBlank()) {
                        state.installedApps
                    } else {
                        state.installedApps.filter {
                            it.appName.contains(
                                state.searchQuery, ignoreCase = true
                            )
                        }
                    }

                    items(
                        items = filteredApps, key = { it.packageName }) { app ->
                        AppToggleItem(
                            appName = app.appName,
                            packageName = app.packageName,
                            isAllowed = app.isAllowed,
                            iconBitmap = app.iconBitmap,
                            onToggle = { isChecked ->
                                onEvent(AppSelectionEvent.ToggleApp(app.packageName, isChecked))
                            })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "App Selection List", locale = "fa")
@Composable
fun AppSelectionPreview() {
    ParentControlTheme {
        AppSelectionContent(
            state = AppSelectionState(
                isLoading = false, searchQuery = "", installedApps = listOf(
                    AppItemUi("com.whatsapp", "واتس‌اپ", true),
                    AppItemUi("com.instagram.android", "اینستاگرام", false),
                    AppItemUi("com.mojang.minecraftpe", "ماینکرافت", true),
                    AppItemUi("com.google.android.youtube", "یوتیوب", false)
                )
            ), onEvent = {})
    }
}