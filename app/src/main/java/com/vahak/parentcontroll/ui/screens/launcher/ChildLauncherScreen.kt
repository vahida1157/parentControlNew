package com.vahak.parentcontroll.ui.screens.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.launcher.LauncherEffect
import com.vahak.parentcontroll.presentation.launcher.LauncherEvent
import com.vahak.parentcontroll.presentation.launcher.LauncherState
import com.vahak.parentcontroll.presentation.launcher.LauncherViewModel
import com.vahak.parentcontroll.ui.component.PinEntryDialog
import com.vahak.parentcontroll.ui.component.launcher.AppDrawerBottomSheet
import com.vahak.parentcontroll.ui.component.launcher.LauncherBottomDock
import com.vahak.parentcontroll.ui.component.launcher.LauncherHeader
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

// --- 1. STATEFUL WRAPPER ---
@Composable
fun ChildLauncherScreen(
    childName: String = "محمدمهدی",
    viewModel: LauncherViewModel = hiltViewModel(),
    onExitLauncherClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // Listen for the "Let me out" effect
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is LauncherEffect.RequestExit) {
                onExitLauncherClick()
            }
        }
    }

    ChildLauncherContent(
        childName = childName,
        state = state,
        onEvent = viewModel::onEvent
    )
}

// --- 2. STATELESS CONTENT ---
@Composable
fun ChildLauncherContent(
    childName: String,
    state: LauncherState,
    onEvent: (LauncherEvent) -> Unit
) {
    val colors = LocalCustomColors.current
    var isDrawerOpen by remember { mutableStateOf(false) }

    val backgroundBrush = Brush.radialGradient(
        colors = listOf(Color(0xFFE8F5E9), colors.background), radius = 1500f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .systemBarsPadding()
    ) {
        // --- THE EXIT DOOR ---
        Card(
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            modifier = Modifier
                .padding(20.dp)
                .size(45.dp)
                .align(Alignment.TopStart)
                // PRO FIX: Fire the event instead of exiting directly
                .clickable { onEvent(LauncherEvent.ExitLauncherClicked) }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = AppIcons.LockBadge,
                    contentDescription = "Exit",
                    tint = colors.textSecondary
                )
            }
        }

        // --- HEADER ---
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LauncherHeader(
                childName = childName,
                modifier = Modifier.padding(top = 80.dp)
            )
        }

        // --- BOTTOM DOCK ---
        LauncherBottomDock(
            modifier = Modifier.align(Alignment.BottomCenter),
            onLeftIconClick = { /* Handle Gallery */ },
            onRightIconClick = { /* Handle Phone */ },
            onCenterDrawerClick = { isDrawerOpen = true }
        )
    }

    // --- APP DRAWER MODAL ---
    if (isDrawerOpen) {
        AppDrawerBottomSheet(
            apps = state.installedApps,
            isLoading = state.isLoading,
            onDismiss = { isDrawerOpen = false },
            onAppClick = { packageName ->
                onEvent(LauncherEvent.AppClicked(packageName))
                isDrawerOpen = false
            }
        )
    }

    // --- EXIT PIN DIALOG ---
    if (state.showExitDialog) {
        PinEntryDialog(
            errorMessage = state.exitErrorMessage,
            onDismiss = { onEvent(LauncherEvent.DismissExitDialog) },
            onSubmit = { pin -> onEvent(LauncherEvent.SubmitExitPin(pin)) }
        )
    }
}

// --- 3. PREVIEW ---
@Preview(showBackground = true, locale = "fa")
@Composable
fun ChildLauncherScreenPreview() {
    ParentControlTheme {
        ChildLauncherContent(
            childName = "محمدمهدی",
            state = LauncherState(installedApps = emptyList(), isLoading = false),
            onEvent = {}
        )
    }
}