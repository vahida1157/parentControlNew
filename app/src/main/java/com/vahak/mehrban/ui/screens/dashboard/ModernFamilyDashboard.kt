package com.vahak.mehrban.ui.screens.dashboard

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.core.util.PermissionChecker
import com.vahak.mehrban.core.util.PermissionType
import com.vahak.mehrban.presentation.dashboard.DashboardEffect
import com.vahak.mehrban.presentation.dashboard.DashboardEvent
import com.vahak.mehrban.presentation.dashboard.DashboardState
import com.vahak.mehrban.presentation.dashboard.DashboardViewModel
import com.vahak.mehrban.ui.component.ChildSelectorCard
import com.vahak.mehrban.ui.component.DashboardHeader
import com.vahak.mehrban.ui.component.DashboardMenuItem
import com.vahak.mehrban.ui.component.SwipeToActivateButton
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme
import java.time.LocalDate
import com.vahak.mehrban.core.data.local.entity.Gender as DbGender

@Composable
fun ModernFamilyDashboard(
    viewModel: DashboardViewModel = hiltViewModel(),
    onAddChildClick: () -> Unit = {},
    onSettingsClick: (String) -> Unit = {},
    onReportClick: (String) -> Unit = {},
    onManageFamilyClick: () -> Unit = {},
    onNavigateToPasswordSetup: () -> Unit = {},
    onLogoutComplete: () -> Unit = {},
    onSecurityFabClick: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DashboardEffect.NavigateToLogin -> onLogoutComplete()
                is DashboardEffect.NavigateToPasswordSetup -> {
                    onNavigateToPasswordSetup()
                }
            }
        }
    }

    ModernFamilyDashboardContent(
        state = state,
        onEvent = viewModel::onEvent,
        onAddChildClick = onAddChildClick,
        onManageFamilyClick = onManageFamilyClick,
        onSettingsClick = onSettingsClick,
        onReportClick = onReportClick,
        onSecurityFabClick = onSecurityFabClick,
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernFamilyDashboardContent(
    state: DashboardState,
    onEvent: (DashboardEvent) -> Unit,
    onAddChildClick: () -> Unit,
    onManageFamilyClick: () -> Unit,
    onSettingsClick: (String) -> Unit,
    onReportClick: (String) -> Unit = {},
    onSecurityFabClick: (String) -> Unit = {},
) {
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Dynamic Permission State
    var missingSecurityPermissions by remember { mutableStateOf(emptyList<PermissionType>()) }
    val isPreview = LocalInspectionMode.current
    // This checks the permissions every time the Dashboard becomes visible on the screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (!isPreview) {
                    val missing = listOf(PermissionType.ACCESSIBILITY, PermissionType.DEVICE_ADMIN)
                        .filter { !PermissionChecker.hasPermission(context, it) }
                    missingSecurityPermissions = missing
                } else {
                    // In preview mode, pretend all permissions are granted
                    missingSecurityPermissions = emptyList()
                }

            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The Bouncing Animation Math
    val infiniteTransition = rememberInfiniteTransition(label = "fab_bounce")
    val fabOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f, // Bounces 15 pixels up
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_offset"
    )
    Scaffold(
        floatingActionButton = {
            // ONLY show the FAB if there are missing permissions!
            if (missingSecurityPermissions.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        // Dynamically generate the string (e.g. "DEVICE_ADMIN", or "DEVICE_ADMIN,ACCESSIBILITY")
                        val permissionsString =
                            missingSecurityPermissions.joinToString(",") { it.name }
                        onSecurityFabClick(permissionsString)
                    },
                    containerColor = colors.red, // Making it red to signify a warning!
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.offset(y = fabOffsetY.dp) // Apply the bounce animation
                ) {
                    Icon(AppIcons.Settings, contentDescription = "تکمیل امنیت")
                }
            }
        },
        containerColor = colors.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                DashboardHeader(
                    onHelpClick = {},
                    onUnlockClick = { onEvent(DashboardEvent.LockClicked) })

                Column(
                    modifier = Modifier.padding(horizontal = 25.dp)
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    ChildSelectorCard(
                        activeChild = state.activeChild,
                        otherChildren = state.children.filter { it.id != state.activeChild?.id },
                        onAddClick = onAddChildClick,
                        onClick = { onEvent(DashboardEvent.OpenChildSheet) })

                    DashboardMenuItem(
                        title = "تنظیمات خانواده", icon = AppIcons.Settings, onClick = {
                            if (state.activeChild != null) {
                                onSettingsClick(state.activeChild.id)
                            } else {
                                onEvent(DashboardEvent.OpenChildSheet)
                            }
                        })

                    DashboardMenuItem(
                        title = "گزارش فعالیت‌ها", icon = AppIcons.ChartBar, onClick = {
                            if (state.activeChild != null) {
                                onReportClick(state.activeChild.id)
                            } else {
                                onEvent(DashboardEvent.OpenChildSheet)
                            }
                        })

                    Spacer(modifier = Modifier.height(20.dp))

                    SwipeToActivateButton(isActive = state.isProtectionActive, onActivate = {
                        if (state.activeChild != null) {
                            onEvent(DashboardEvent.ActivateProtection(state.activeChild.id))
                        } else {
                            onEvent(DashboardEvent.OpenChildSheet)
                        }
                    }, onDeactivate = {
//                        if (state.activeChild != null) {
//                            onEvent(DashboardEvent.DeactivateProtection(state.activeChild.id))
//                        }
                    })

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // --- BOTTOM SHEET ---
    if (state.isChildSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(DashboardEvent.CloseChildSheet) },
            containerColor = colors.surface
        ) {
            Column(modifier = Modifier.padding(25.dp)) {
                Text(
                    "انتخاب فرزند",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(15.dp))

                if (state.children.isEmpty()) {
                    Text(
                        "هنوز فرزندی ثبت نشده است.",
                        color = colors.textHint,
                        modifier = Modifier.padding(15.dp)
                    )
                } else {
                    state.children.forEach { child ->
                        val isSelected = child.id == state.activeChild?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEvent(DashboardEvent.SelectChild(child)) }
                                .padding(vertical = 15.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .background(
                                            if (isSelected) colors.yellow.copy(alpha = 0.2f) else colors.blue.copy(
                                                alpha = 0.2f
                                            ), CircleShape
                                        ), contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        AppIcons.YoungChild,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.yellow else colors.blue
                                    )
                                }
                                Spacer(modifier = Modifier.width(15.dp))
                                Text(
                                    text = child.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    AppIcons.Check, contentDescription = null, tint = colors.primary
                                )
                            }
                        }
                        HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                val hasChildren = state.children.isNotEmpty()

                Button(
                    onClick = {
                        onEvent(DashboardEvent.CloseChildSheet)
                        if (hasChildren) {
                            onManageFamilyClick()
                        } else {
                            onAddChildClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(15.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (hasChildren) {
                            Icon(
                                painter = AppIcons.Profile,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Text(
                            text = if (hasChildren) "مدیریت فرزندان" else "افزودن فرزند جدید",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // --- NEW: PIN REQUIRED DIALOG ---
    if (state.showPinRequiredDialog) {
        Dialog(onDismissRequest = { onEvent(DashboardEvent.ClosePinRequiredDialog) }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(colors.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AppIcons.LockBadge,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "تنظیم رمز عبور الزامی است",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "برای فعال‌سازی حالت محافظت و قفل کردن محیط کودک، ابتدا باید رمز عبور والدین را تنظیم کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(25.dp))

                    Button(
                        onClick = { onEvent(DashboardEvent.GoToPasswordSetupClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تنظیم رمز عبور", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = { onEvent(DashboardEvent.ClosePinRequiredDialog) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("انصراف", color = colors.textHint, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================
private val mockChild1 =
    ChildEntity(id = "1", name = "محمد", dob = LocalDate.of(2015, 5, 20), gender = DbGender.BOY)
private val mockChild2 =
    ChildEntity(id = "2", name = "محمدمهدی", dob = LocalDate.of(2018, 8, 15), gender = DbGender.BOY)

@Preview(showBackground = true, name = "1. Dashboard (No Children)", locale = "fa", heightDp = 800)
@Composable
fun DashboardPreviewEmpty() {
    ParentControlTheme {
        ModernFamilyDashboardContent(
            state = DashboardState(
                children = emptyList(), activeChild = null
            ), onEvent = {}, onAddChildClick = {}, onManageFamilyClick = {}, onSettingsClick = {})
    }
}

@Preview(showBackground = true, name = "2. Dashboard (Populated)", locale = "fa", heightDp = 800)
@Composable
fun DashboardPreviewPopulated() {
    ParentControlTheme {
        ModernFamilyDashboardContent(
            state = DashboardState(
                children = listOf(mockChild1, mockChild2), activeChild = mockChild2
            ), onEvent = {}, onAddChildClick = {}, onManageFamilyClick = {}, onSettingsClick = {})
    }
}

@Preview(showBackground = true, name = "3. Dashboard (Dialog Open)", locale = "fa", heightDp = 800)
@Composable
fun DashboardPreviewDialog() {
    ParentControlTheme {
        ModernFamilyDashboardContent(
            state = DashboardState(
                children = listOf(mockChild1),
                activeChild = mockChild1,
                showPinRequiredDialog = true
            ), onEvent = {}, onAddChildClick = {}, onManageFamilyClick = {}, onSettingsClick = {})
    }
}