package com.vahak.mehrban.uiv2.screens.dashboard

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.core.data.local.entity.Gender
import com.vahak.mehrban.core.util.PermissionType
import com.vahak.mehrban.presentation.dashboard.DashboardEffect
import com.vahak.mehrban.presentation.dashboard.DashboardEvent
import com.vahak.mehrban.presentation.dashboard.DashboardState
import com.vahak.mehrban.presentation.dashboard.DashboardViewModel
import com.vahak.mehrban.ui.component.SwipeToActivateButton
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.Period

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onAddChildClick: () -> Unit,
    onManageFamilyClick: () -> Unit,
    onSettingsClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onTimeLockClick: (String) -> Unit,
    onNavigateToPasswordSetup: () -> Unit,
    onLogoutComplete: () -> Unit,
    onSecurityFabClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DashboardEffect.NavigateToLogin -> onLogoutComplete()
                is DashboardEffect.NavigateToPasswordSetup -> onNavigateToPasswordSetup()
            }
        }
    }

    DashboardScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onAddChildClick = onAddChildClick,
        onManageFamilyClick = onManageFamilyClick,
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
    onEvent: (DashboardEvent) -> Unit,
    onAddChildClick: () -> Unit,
    onManageFamilyClick: () -> Unit,
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
    // Local State for HTML's Launcher Confirmation Modal
    var showLauncherConfirmSheet by remember { mutableStateOf(false) }

    // Dynamic Permission State
    var missingSecurityPermissions by remember { mutableStateOf(emptyList<PermissionType>()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (isPreview) {
                    missingSecurityPermissions = emptyList() // Mock state for previews
                } else {
                    // FIXME: Currently we commented out the permission for accessibility and device admin for publish issues,
//                    val missing = listOf(
//                        PermissionType.ACCESSIBILITY, PermissionType.DEVICE_ADMIN
//                    ).filter { !PermissionChecker.hasPermission(context, it) }
                    missingSecurityPermissions = listOf()
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
            if (missingSecurityPermissions.isNotEmpty())
                FloatingActionButton(
                    onClick = {
                        val permissionsString =
                            missingSecurityPermissions.joinToString(",") { it.name }
                        onSecurityFabClick(permissionsString)
                    },
                    containerColor = colors.red,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .offset(y = fabOffsetY.dp)
                ) {
                    Icon(AppIcons.Settings, contentDescription = "تکمیل امنیت")
                }
        }) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            DashboardHeaderV2(onProfileClick = onProfileClick)

            if (state.children.isEmpty() || state.activeChild == null) {
                // Show massive CTA when no child exists
                EmptyDashboardStateV2(onAddClick = onAddChildClick)
            } else {
                // Show normal dashboard when children exist
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
                    onSettingsClick = { onSettingsClick(state.activeChild.id) }
                )

                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                    SwipeToActivateButton(
                        isActive = state.isProtectionActive,
                        onActivate = { showLauncherConfirmSheet = true },
                        onDeactivate = { /* Handle deactivation */ }
                    )
                }

                ActionGridV2(
                    onSettingsClick = { onSettingsClick(state.activeChild.id) },
                    onReportClick = { onReportClick(state.activeChild.id) },
                    onTimeLockClick = { onTimeLockClick(state.activeChild.id) },
                    onLocationClick = {
                        Toast.makeText(context, "به زودی در دسترس خواهد بود", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            BannerSliderV2()

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // --- LAUNCHER CONFIRMATION BOTTOM SHEET (Matches HTML #modal-launcher) ---
    if (showLauncherConfirmSheet && state.activeChild != null) {
        ModalBottomSheet(
            onDismissRequest = { showLauncherConfirmSheet = false },
            containerColor = colors.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Shield Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(listOf(colors.red, Color(0xFFD44245))),
                            RoundedCornerShape(24.dp)
                        )
                        .shadow(
                            16.dp,
                            RoundedCornerShape(24.dp),
                            spotColor = colors.red.copy(alpha = 0.4f)
                        ), contentAlignment = Alignment.Center
                ) {
                    Text("🛡️", fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "فعال‌سازی لانچر امن",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "پس از فعال‌سازی، دستگاه فقط برنامه‌های تأیید شده را نمایش می‌دهد و خروج از این حالت نیاز به رمز والد دارد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Active Child Confirmation Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surface, RoundedCornerShape(16.dp))
                        .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(colors.cardInnerBG, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.activeChild.gender == Gender.BOY) "👦" else "👧",
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("فعال‌سازی برای فرزند", fontSize = 11.sp, color = colors.textSecondary)
                        Text(
                            state.activeChild.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.primary
                        )
                    }
                    TextButton(onClick = {
                        showLauncherConfirmSheet = false
                        onEvent(DashboardEvent.OpenChildSheet)
                    }) {
                        Text(
                            "تغییر",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Warning Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.orangeLight, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.orange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp), verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⚠️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "برای خروج از حالت لانچر، رمز ۴ الی 8 رقمی والد را در دستگاه فرزند وارد کنید.",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        showLauncherConfirmSheet = false
                        onEvent(DashboardEvent.ActivateProtection(state.activeChild.id))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            8.dp,
                            RoundedCornerShape(14.dp),
                            spotColor = colors.red.copy(alpha = 0.3f)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        colors.red, Color(0xFFD44245)
                                    )
                                )
                            ), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🛡️ فعال‌سازی حالت لانچر",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }

                TextButton(
                    onClick = { showLauncherConfirmSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("انصراف", color = colors.textSecondary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- V2 PIN REQUIRED DIALOG (Triggered by ViewModel if PIN is missing) ---
    if (state.showPinRequiredDialog) {
        Dialog(onDismissRequest = { onEvent(DashboardEvent.ClosePinRequiredDialog) }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(colors.primary.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, colors.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AppIcons.LockBadge,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "تنظیم رمز عبور الزامی است",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "برای فعال‌سازی حالت محافظت و قفل کردن محیط کودک، ابتدا باید رمز عبور والدین را تنظیم کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { onEvent(DashboardEvent.GoToPasswordSetupClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "تنظیم رمز عبور",
                            color = colors.textOnPrimaryVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
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

// ----------------------------------------------------------------------------
// EXTRACTED UI COMPONENTS
// ----------------------------------------------------------------------------

@Composable
fun DashboardHeaderV2(onProfileClick: () -> Unit) {
    val colors = LocalCustomColors.current
    val headerGradient = Brush.linearGradient(listOf(colors.primary, colors.primaryVariant))
    val avatarGradient = Brush.linearGradient(listOf(colors.yellow, colors.orange))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = headerGradient,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(top = 20.dp, bottom = 30.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("سلام 👋", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Text(
                    "والد گرامی",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(avatarGradient, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable { onProfileClick() }, // <-- 2. Call it here
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    AppIcons.Profile,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HomeChildSelectorV2(
    children: List<ChildEntity>,
    activeChild: ChildEntity?,
    onSelect: (ChildEntity) -> Unit,
    onAddClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Column(modifier = Modifier.padding(top = 16.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)) {
        Text(
            text = "انتخاب فرزند",
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 20.dp)
        ) {
            items(children) { child ->
                val isSelected = child.id == activeChild?.id
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface)
                        .border(
                            2.dp,
                            if (isSelected) colors.primary else Color.Transparent,
                            RoundedCornerShape(30.dp)
                        )
                        .clickable { onSelect(child) }
                        .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.cardInnerBG, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (child.gender == Gender.BOY) "👦" else "👧", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = child.name,
                        color = if (isSelected) colors.primary else colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(colors.surface, CircleShape)
                        .border(2.dp, colors.primary.copy(alpha = 0.5f), CircleShape)
                        .clickable { onAddClick() }, contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+", color = colors.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDashboardStateV2(onAddClick: () -> Unit) {
    val colors = LocalCustomColors.current
    val strokeColor = colors.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primary.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                .drawBehind {
                    val stroke = Stroke(
                        width = 5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 20f), 0f)
                    )
                    drawRoundRect(
                        color = strokeColor.copy(alpha = 0.3f),
                        style = stroke,
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
                .clip(RoundedCornerShape(24.dp))
                .clickable { onAddClick() }
                .padding(vertical = 48.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Big Floating Plus Icon
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            Brush.linearGradient(listOf(colors.primary, colors.primaryVariant)),
                            CircleShape
                        )
                        .shadow(12.dp, CircleShape, spotColor = colors.primary.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        AppIcons.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "اولین فرزند خود را اضافه کنید",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = colors.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "برای دسترسی به تنظیمات، گزارش‌ها و ابزارهای محافظتی، ابتدا پروفایل فرزندتان را بسازید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "افزودن فرزند",
                        color = colors.textOnPrimaryVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveChildSummaryCardV2(
    child: ChildEntity,
    timeLimitMins: Int,
    isTimeLimitActive: Boolean,
    usageSeconds: Int,
    onSettingsClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    val age = Period.between(child.dob, LocalDate.now()).years
    val ageText = if (age > 0) "$age ساله" else "کمتر از یک سال"

    val usageHours = usageSeconds / 3600
    val usageMins = (usageSeconds % 3600) / 60
    val formattedUsage = String.format("%d:%02d", usageHours, usageMins)

    val limitHours = timeLimitMins / 60
    val limitMins = timeLimitMins % 60
    val formattedLimit = if (!isTimeLimitActive || timeLimitMins == 0) {
        "نامحدود"
    } else {
        buildString {
            if (limitHours > 0) append("$limitHours ساعت ")
            if (limitMins > 0) append("$limitMins دقیقه")
        }.trim().ifEmpty { "نامحدود" }
    }

    val totalLimitSeconds = (timeLimitMins * 60).toFloat().coerceAtLeast(1f)

    // 🚀 BUG FIX: If the limit is off, force progress to 0 so the bar stays empty!
    val progress = if (!isTimeLimitActive || timeLimitMins == 0) {
        0f
    } else {
        (usageSeconds.toFloat() / totalLimitSeconds).coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(colors.cardInnerBG, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (child.gender == Gender.BOY) "👦" else "👧", fontSize = 32.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        child.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        ageText,
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        AppIcons.Settings,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "زمان استفاده امروز",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isTimeLimitActive && timeLimitMins > 0) "$formattedUsage از $formattedLimit" else "$formattedUsage (نامحدود)",
                    fontSize = 12.sp, fontWeight = FontWeight.Black, color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = when {
                    !isTimeLimitActive || timeLimitMins == 0 -> colors.divider // 🚀 Force neutral color when unlimited
                    progress > 0.9f -> colors.red
                    progress > 0.75f -> colors.yellow
                    else -> colors.primary
                },
                trackColor = colors.divider,
            )
        }
    }
}

@Composable
fun MiniStatItem(value: String, label: String) {
    val colors = LocalCustomColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 10.sp, color = colors.textSecondary)
    }
}

@Composable
fun LauncherCTAButtonV2(childName: String, isProtectionActive: Boolean, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    // Change color to green if active
    val bgGradient = if (isProtectionActive) {
        Brush.linearGradient(listOf(colors.green, Color(0xFF0D9488)))
    } else {
        Brush.linearGradient(listOf(colors.red, Color(0xFFD44245)))
    }

    Button(
        // Disable clicking if it is already active
        onClick = { if (!isProtectionActive) onClick() },
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .height(84.dp)
            .padding(bottom = 16.dp)
            .shadow(
                12.dp,
                RoundedCornerShape(16.dp),
                spotColor = (if (isProtectionActive) colors.green else colors.red).copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isProtectionActive) "✅" else "🛡️", fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isProtectionActive) "محیط امن فعال است" else "ورود به حالت لانچر فرزند",
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        if (isProtectionActive) "در حال محافظت از $childName" else "فعال‌سازی محیط امن برای $childName",
                        color = Color.White.copy(alpha = 0.95f), fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActionGridV2(
    onSettingsClick: () -> Unit,
    onReportClick: () -> Unit,
    onTimeLockClick: () -> Unit,
    onLocationClick: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionCardV2(
                "تنظیمات کامل",
                "زمان، فیلتر و قفل",
                "⚙️",
                Color(0xFFE3F2FD),
                Color(0xFF1976D2),
                modifier = Modifier.weight(1f),
                onClick = onSettingsClick
            )
            ActionCardV2(
                "گزارش عملکرد",
                "آمار و فعالیت هفتگی",
                "📊",
                Color(0xFFE8F5E9),
                Color(0xFF27AE60),
                modifier = Modifier.weight(1f),
                onClick = onReportClick
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionCardV2(
                "قفل زمان فوری",
                "قفل دستگاه همین حالا",
                "⏱️",
                Color(0xFFFFF3E0),
                Color(0xFFE65100),
                modifier = Modifier.weight(1f),
                onClick = onTimeLockClick
            )
            ActionCardV2(
                "موقعیت لحظه‌ای",
                "مشاهده روی نقشه",
                "📍",
                Color(0xFFFCE4EC),
                Color(0xFFC2185B),
                modifier = Modifier.weight(1f),
                onClick = onLocationClick
            )
        }
    }
}

@Composable
fun ActionCardV2(
    title: String,
    desc: String,
    emoji: String,
    iconBg: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 18.sp, color = iconColor)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = colors.textPrimary
                )
                Text(desc, fontSize = 10.sp, color = colors.textSecondary, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerSliderV2() {
    val colors = LocalCustomColors.current

    val banners = listOf(
        Triple("📢", "کانال رسمی مهربان", "آخرین بروزرسانی‌ها و نکات والدگری"),
        Triple("💬", "پشتیبانی ۲۴ ساعته", "سوال یا مشکلی دارید؟ تیم پشتیبانی در خدمت شماست"),
        Triple("🎁", "پیشنهاد ویژه اشتراک", "۳۰٪ تخفیف اشتراک سالانه مهربان پلاس")
    )

    // Move pageCount to use the dynamic size of your list
    val pagerState = rememberPagerState(pageCount = { banners.size })

    // --- BULLETPROOF Auto-scroll logic ---
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000L) // Wait for 10 seconds

            // Only auto-scroll if the user isn't actively swiping
            if (!pagerState.isScrollInProgress) {
                try {
                    // 1. Use settledPage (the last page it fully stopped on) instead of currentPage
                    val nextPage = (pagerState.settledPage + 1) % banners.size

                    // 2. Use a strict tween animation to override the default Spring physics
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = tween(
                            durationMillis = 800,
                            easing = FastOutSlowInEasing
                        )
                    )
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // 3. IMPORTANT: If the user touches the banner while it's auto-scrolling,
                    // Compose throws a CancellationException. If we don't catch it,
                    // the while(true) loop dies permanently!
                }
            }
        }
    }


    val bannerColors = listOf(
        Brush.linearGradient(listOf(colors.primary, colors.primaryVariant)),
        Brush.linearGradient(listOf(colors.yellow, Color(0xFFC49530))),
        Brush.linearGradient(listOf(colors.blue, Color(0xFF4C51BF))),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
            ) { page ->
                val banner = banners[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bannerColors[page])
                        .clickable {
                            // TODO/FIXME: Redirect to actual Web URL or Channel Link in the future
                        }
                        .padding(18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)
                                ), contentAlignment = Alignment.Center
                        ) {
                            Text(banner.first, fontSize = 30.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                banner.second,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                banner.third,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(banners.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (isSelected) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary else colors.divider)
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

private val mockChild1 =
    ChildEntity(id = "1", name = "علی", dob = LocalDate.of(2009, 1, 1), gender = Gender.BOY)
private val mockChild2 =
    ChildEntity(id = "2", name = "سارا", dob = LocalDate.now(), gender = Gender.GIRL)

@Preview(showBackground = true, locale = "fa", name = "1. Dashboard V2 - Empty", apiLevel = 34)
@Composable
fun DashboardScreenPreviewEmpty() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        DashboardScreenContent(
            state = DashboardState(children = emptyList(), activeChild = null),
            onEvent = {},
            onAddChildClick = {},
            onManageFamilyClick = {},
            onSettingsClick = {},
            onReportClick = {},
            onTimeLockClick = {},
            onSecurityFabClick = {},
            onProfileClick = {})
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Dashboard V2 - Populated (Light)")
@Composable
fun DashboardScreenPreviewPopulatedLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        DashboardScreenContent(
            state = DashboardState(
                children = listOf(mockChild1, mockChild2), activeChild = mockChild1,
                activeChildTimeLimitMins = 50, activeChildUsageSeconds = 1700
            ),
            onEvent = {},
            onAddChildClick = {},
            onManageFamilyClick = {},
            onSettingsClick = {},
            onReportClick = {},
            onTimeLockClick = {},
            onSecurityFabClick = {},
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
            onManageFamilyClick = {},
            onSettingsClick = {},
            onReportClick = {},
            onTimeLockClick = {},
            onSecurityFabClick = {},
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
            onManageFamilyClick = {},
            onSettingsClick = {},
            onReportClick = {},
            onTimeLockClick = {},
            onSecurityFabClick = {},
            onProfileClick = {})
    }
}