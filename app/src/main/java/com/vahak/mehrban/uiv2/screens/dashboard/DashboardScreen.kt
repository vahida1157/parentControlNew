package com.vahak.mehrban.uiv2.screens.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vahak.mehrban.BuildConfig
import com.vahak.mehrban.R
import com.vahak.mehrban.UpdateState
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.core.data.local.entity.Gender
import com.vahak.mehrban.core.util.PermissionChecker
import com.vahak.mehrban.core.util.PermissionType
import com.vahak.mehrban.presentation.dashboard.DashboardEffect
import com.vahak.mehrban.presentation.dashboard.DashboardEvent
import com.vahak.mehrban.presentation.dashboard.DashboardState
import com.vahak.mehrban.presentation.dashboard.DashboardViewModel
import com.vahak.mehrban.ui.component.SwipeToActivateButton
import com.vahak.mehrban.uiv2.components.SmartRatingDialog
import com.vahak.mehrban.uiv2.components.dashboard.ActionGridV2
import com.vahak.mehrban.uiv2.components.dashboard.ActiveChildSummaryCardV2
import com.vahak.mehrban.uiv2.components.dashboard.BannerSliderV2
import com.vahak.mehrban.uiv2.components.dashboard.EmptyDashboardStateV2
import com.vahak.mehrban.uiv2.components.dashboard.HomeChildSelectorV2
import com.vahak.mehrban.uiv2.components.dashboard.LauncherConfirmSheet
import com.vahak.mehrban.uiv2.components.dashboard.PinRequiredDialog
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import java.time.LocalDate

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onAddChildClick: () -> Unit,
    onSettingsClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onTimeLockClick: (String) -> Unit,
    onNavigateToPasswordSetup: () -> Unit,
    onLogoutComplete: () -> Unit,
    onSecurityFabClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val isUpdateIgnored by viewModel.isUpdateIgnored.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DashboardEffect.NavigateToLogin -> onLogoutComplete()
                is DashboardEffect.NavigateToPasswordSetup -> onNavigateToPasswordSetup()

                // 🚀 NEW: Handle Rating Dialog App Store Navigation dynamically!
                is DashboardEffect.OpenAppStoreRating -> {
                    val installSource = BuildConfig.INSTALL_SOURCE
                    try {
                        when (installSource) {
                            "bazaar" -> {
                                val intent = Intent(Intent.ACTION_EDIT).apply {
                                    data =
                                        "bazaar://details?id=${context.packageName}".toUri()
                                    setPackage("com.farsitel.bazaar")
                                }
                                context.startActivity(intent)
                            }

                            "myket" -> {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data =
                                        "myket://details?id=${context.packageName}".toUri()
                                    setPackage("ir.mservices.market")
                                }
                                context.startActivity(intent)
                            }

                            "googleplay" -> {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data =
                                        "market://details?id=${context.packageName}".toUri()
                                    setPackage("com.android.vending")
                                }
                                context.startActivity(intent)
                            }

                            else -> {} // Website build - do nothing
                        }
                    } catch (_: Exception) {
                        // Fallback to web browser if store app is missing
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/details?id=${context.packageName}".toUri()
                        )
                        context.startActivity(intent)
                    }
                }

                // 🚀 NEW: Handle Eitaa support intent
                is DashboardEffect.OpenSupportAccount -> {
                    try {
                        val intent =
                            Intent(Intent.ACTION_VIEW, "https://eitaa.com/mehrbansupport".toUri())
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    DashboardScreenContent(
        state = state,
        updateState = updateState,
        isUpdateIgnored = isUpdateIgnored,
        onEvent = viewModel::onEvent,
        onShowUpdateDialogAgain = viewModel::showUpdateDialogAgain,
        onAddChildClick = onAddChildClick,
        onSettingsClick = onSettingsClick,
        onReportClick = onReportClick,
        onTimeLockClick = onTimeLockClick,
        onSecurityFabClick = onSecurityFabClick,
        onProfileClick = onProfileClick,
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreenContent(
    state: DashboardState,
    updateState: UpdateState,
    isUpdateIgnored: Boolean,
    onEvent: (DashboardEvent) -> Unit,
    onShowUpdateDialogAgain: () -> Unit,
    onAddChildClick: () -> Unit,
    onSettingsClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onTimeLockClick: (String) -> Unit,
    onSecurityFabClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPreview = androidx.compose.ui.platform.LocalInspectionMode.current

    var showLauncherConfirmSheet by remember { mutableStateOf(false) }
    var missingSecurityPermissions by remember { mutableStateOf(emptyList<PermissionType>()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isPreview) {
                    missingSecurityPermissions = emptyList()
                } else {
                    val requiredPermissions = mutableListOf<PermissionType>()
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        requiredPermissions.add(PermissionType.NOTIFICATIONS)
                    }
                    missingSecurityPermissions = requiredPermissions.filter { permission ->
                        !PermissionChecker.hasPermission(context, permission)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fab_bounce")
    val fabOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -15f, animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        ), label = "fab_offset"
    )

    Scaffold(
        containerColor = colors.background, floatingActionButton = {
            if (missingSecurityPermissions.isNotEmpty()) FloatingActionButton(
                onClick = {
                    val permissionsString = missingSecurityPermissions.joinToString(",") { it.name }
                    onSecurityFabClick(permissionsString)
                }, containerColor = colors.red, contentColor = Color.White, shape = CircleShape,
                modifier = Modifier.graphicsLayer { translationY = fabOffsetY.dp.toPx() }) {
                Icon(
                    AppIcons.Settings,
                    contentDescription = stringResource(R.string.complete_security)
                )
            }
        }) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (isUpdateIgnored && updateState is UpdateState.UpdateAvailable) {
                UpdateBanner(onShowUpdateDialogAgain = onShowUpdateDialogAgain)
            }

            MehrbanHeader(
                title = stringResource(R.string.dear_parent),
                subtitle = stringResource(R.string.hello_greeting),
                action = HeaderAction.Profile(onClick = onProfileClick),
            )

            if (state.children.isEmpty() || state.activeChild == null) {
                EmptyDashboardStateV2(onAddClick = onAddChildClick)
            } else {
                HomeChildSelectorV2(
                    children = state.children,
                    activeChild = state.activeChild,
                    onSelect = { onEvent(DashboardEvent.SelectChild(it)) },
                    onAddClick = onAddChildClick
                )

                ActiveChildSummaryCardV2(
                    child = state.activeChild,
                    timeLimitMins = state.activeChildTimeLimitMins,
                    isTimeLimitActive = state.isTimeLimitActive,
                    usageSeconds = state.activeChildUsageSeconds,
                    onSettingsClick = { onSettingsClick(state.activeChild.id) })

                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                    SwipeToActivateButton(
                        isActive = state.isProtectionActive,
                        onActivate = { showLauncherConfirmSheet = true },
                        onDeactivate = { /* Handle deactivation */ })
                }

                val comingSoonMessage = stringResource(R.string.coming_soon)
                ActionGridV2(
                    onSettingsClick = { onSettingsClick(state.activeChild.id) },
                    onReportClick = { onReportClick(state.activeChild.id) },
                    onTimeLockClick = { onTimeLockClick(state.activeChild.id) },
                    onLocationClick = {
                        Toast.makeText(
                            context,
                            comingSoonMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            BannerSliderV2()
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // --- DIALOGS & SHEETS ---
    if (showLauncherConfirmSheet && state.activeChild != null) {
        LauncherConfirmSheet(
            activeChild = state.activeChild,
            onDismiss = { showLauncherConfirmSheet = false },
            onChangeChildClick = {
                showLauncherConfirmSheet = false
                onEvent(DashboardEvent.OpenChildSheet)
            },
            onActivateClick = {
                showLauncherConfirmSheet = false
                onEvent(DashboardEvent.ActivateProtection(state.activeChild.id))
            })
    }

    if (state.showPinRequiredDialog) {
        PinRequiredDialog(
            onDismiss = { onEvent(DashboardEvent.ClosePinRequiredDialog) },
            onSetupPassword = { onEvent(DashboardEvent.GoToPasswordSetupClicked) })
    }

    // 🚀 NEW: Soft Rating Prompt Logic
    if (state.showRatingPrompt && BuildConfig.INSTALL_SOURCE != "website") {
        val title = when (state.ratingPromptStep) {
            1 -> stringResource(R.string.rating_step1_title)
            2 -> stringResource(R.string.rating_step2_title)
            else -> stringResource(R.string.rating_step3_title)
        }

        val desc = when (state.ratingPromptStep) {
            1 -> stringResource(R.string.rating_step1_desc)
            2 -> stringResource(R.string.rating_step2_desc)
            else -> stringResource(R.string.rating_step3_desc)
        }

        val dismissText = if (state.ratingPromptStep == 1) {
            stringResource(R.string.remind_me_later)
        } else {
            stringResource(R.string.no_thanks)
        }
        SmartRatingDialog(
            step = state.ratingPromptStep,
            title = title,
            description = desc,
            dismissText = dismissText,
            yesText = stringResource(R.string.yes),
            noText = stringResource(R.string.no),
            rateText = stringResource(R.string.rate_5_stars),
            feedbackText = stringResource(R.string.send_feedback),
            onSatisfied = { onEvent(DashboardEvent.RatingPromptSatisfied) },
            onDissatisfied = { onEvent(DashboardEvent.RatingPromptDissatisfied) },
            onRateClicked = { onEvent(DashboardEvent.RatingPromptRateClicked) },
            onFeedbackClicked = { onEvent(DashboardEvent.RatingPromptFeedbackClicked) },
            onDismiss = { onEvent(DashboardEvent.DismissRatingPrompt) }
        )
    }
}

@Composable
private fun UpdateBanner(onShowUpdateDialogAgain: () -> Unit) {
    val colors = LocalCustomColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f, animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse
        ), label = "sparkle_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.orangeLight)
            .clickable { onShowUpdateDialogAgain() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
        // 🚀 PRO FIX: graphicsLayer limits animation redraw to just the emoji text
        Text(
            "✨",
            fontSize = 16.sp,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.update_available_banner),
            color = colors.orange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

// ==========================================
// PREVIEWS
// ==========================================

private val mockChild1 =
    ChildEntity(id = "1", name = "علی", dob = LocalDate.of(2009, 1, 1), gender = Gender.BOY)
private val mockChild2 =
    ChildEntity(id = "2", name = "سارا", dob = LocalDate.now(), gender = Gender.GIRL)

@Preview(showBackground = true, locale = "fa", name = "1. Dashboard V2 - Empty")
@Composable
fun DashboardScreenPreviewEmpty() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        DashboardScreenContent(
            state = DashboardState(children = emptyList(), activeChild = null),
            onEvent = {},
            onAddChildClick = {},
            onSettingsClick = {},
            onReportClick = {},
            onTimeLockClick = {},
            onSecurityFabClick = {},
            updateState = UpdateState.UpToDate,
            isUpdateIgnored = false,
            onShowUpdateDialogAgain = {},
            onProfileClick = {})
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Dashboard V2 - Populated (Light)")
@Composable
fun DashboardScreenPreviewPopulatedLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        DashboardScreenContent(
            state = DashboardState(
                children = listOf(mockChild1, mockChild2),
                activeChild = mockChild1,
                activeChildTimeLimitMins = 50,
                activeChildUsageSeconds = 1700
            ),
            onEvent = {},
            onAddChildClick = {},
            onSettingsClick = {},
            onReportClick = {},
            onTimeLockClick = {},
            onSecurityFabClick = {},
            updateState = UpdateState.UpToDate,
            isUpdateIgnored = false,
            onShowUpdateDialogAgain = {},
            onProfileClick = {})
    }
}

@Preview(showBackground = true, locale = "fa", name = "3. Dashboard V2 - Populated (Dark)")
@Composable
fun DashboardScreenPreviewPopulatedDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        DashboardScreenContent(
            state = DashboardState(
                children = listOf(mockChild1, mockChild2), activeChild = mockChild1
            ),
            onEvent = {},
            onAddChildClick = {},
            onSettingsClick = {},
            onReportClick = {},
            onTimeLockClick = {},
            onSecurityFabClick = {},
            updateState = UpdateState.Checking,
            isUpdateIgnored = false,
            onShowUpdateDialogAgain = {},
            onProfileClick = {})
    }
}

@Preview(showBackground = true, locale = "fa", name = "4. Dashboard V2 - PIN Dialog")
@Composable
fun DashboardScreenPreviewDialog() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        DashboardScreenContent(
            state = DashboardState(
                children = listOf(mockChild1),
                activeChild = mockChild1,
                showPinRequiredDialog = true,
            ),
            onEvent = {},
            onAddChildClick = {},
            onSettingsClick = {},
            onReportClick = {},
            onTimeLockClick = {},
            onSecurityFabClick = {},
            updateState = UpdateState.UpToDate,
            isUpdateIgnored = false,
            onShowUpdateDialogAgain = {},
            onProfileClick = {})
    }
}