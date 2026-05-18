package com.vahak.parentcontroll.uiv2.screens.permissions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.core.receiver.SecurityAdminReceiver
import com.vahak.parentcontroll.core.util.PermissionChecker
import com.vahak.parentcontroll.core.util.PermissionType
import com.vahak.parentcontroll.presentation.permissions.PermissionSliderEffect
import com.vahak.parentcontroll.presentation.permissions.PermissionSliderEvent
import com.vahak.parentcontroll.presentation.permissions.PermissionSliderViewModel
import com.vahak.parentcontroll.uiv2.theme.AppIcons
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme

@Composable
fun PermissionSliderScreen(
    viewModel: PermissionSliderViewModel = hiltViewModel(),
    onNavigateToFeature: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var checkTrigger by remember { mutableIntStateOf(0) }
    var currentPermissionToCheck by remember { mutableStateOf<PermissionType?>(null) }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        checkTrigger++
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PermissionSliderEffect.NavigateToFeature -> {
                    onNavigateToFeature(effect.route)
                }

                is PermissionSliderEffect.LaunchAndroidSettings -> {
                    currentPermissionToCheck = effect.permission

                    if (effect.permission == PermissionType.VPN) {
                        val vpnIntent = VpnService.prepare(context)
                        if (vpnIntent != null) {
                            settingsLauncher.launch(vpnIntent)
                        } else {
                            checkTrigger++
                        }
                        return@collect
                    }

                    if (effect.permission == PermissionType.ACCESSIBILITY || effect.permission == PermissionType.DEVICE_ADMIN) {
                        val prefs =
                            context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
                        prefs.edit { putBoolean("settings_bridge_open", true) }
                    }

                    val intent = Intent(effect.action).apply {
                        when (effect.permission) {
                            PermissionType.OVERLAY -> {
                                data = Uri.parse("package:${context.packageName}")
                            }

                            PermissionType.DEVICE_ADMIN -> {
                                val adminComponent =
                                    ComponentName(context, SecurityAdminReceiver::class.java)
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "برای جلوگیری از حذف برنامه توسط کودک، این دسترسی الزامی است."
                                )
                            }

                            else -> {}
                        }
                    }
                    settingsLauncher.launch(intent)
                }
            }
        }
    }

    if (state.missingPermissions.isNotEmpty()) {
        PermissionSliderContent(
            permissions = state.missingPermissions,
            checkTrigger = checkTrigger,
            permissionToCheck = currentPermissionToCheck,
            onGrantClick = { permission ->
                viewModel.onEvent(PermissionSliderEvent.GrantClicked(permission))
            },
            onFinishSetup = { viewModel.onEvent(PermissionSliderEvent.SetupFinished) }
        )
    }
}

@Composable
fun PermissionSliderContent(
    permissions: List<PermissionType>,
    checkTrigger: Int = 0,
    permissionToCheck: PermissionType? = null,
    onGrantClick: (PermissionType) -> Unit,
    onFinishSetup: () -> Unit,
) {
    val colors = LocalCustomColors.current
    val pagerState = rememberPagerState(pageCount = { permissions.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    LaunchedEffect(checkTrigger) {
        if (checkTrigger > 0 && permissionToCheck != null) {
            val isGranted =
                if (isPreview) true else PermissionChecker.hasPermission(context, permissionToCheck)
            if (isGranted) {
                val currentPage = pagerState.currentPage
                if (currentPage < permissions.lastIndex) {
                    pagerState.animateScrollToPage(currentPage + 1)
                } else {
                    onFinishSetup()
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        containerColor = colors.background,
        bottomBar = {
            val isLastPage = pagerState.currentPage == permissions.lastIndex
            PermissionFooterV2(
                buttonText = "اعطای دسترسی",
                onGrantClick = { onGrantClick(permissions[pagerState.currentPage]) },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header with animated progress bar
            PermissionHeaderV2(
                currentPage = pagerState.currentPage + 1,
                totalPages = permissions.size
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) { page ->
                val currentPermission = permissions[page]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Emoji/Icon representing the permission
                    Box(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .size(72.dp)
                            .background(colors.cardInnerBG, RoundedCornerShape(20.dp))
                            .border(
                                2.dp,
                                colors.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconEmoji = when (currentPermission) {
                            PermissionType.ACCESSIBILITY -> "👁️"
                            PermissionType.USAGE_STATS -> "📊"
                            PermissionType.DEVICE_ADMIN -> "🛡️"
                            PermissionType.VPN -> "🌐"
                            PermissionType.OVERLAY -> "📱"
                            PermissionType.LOCATION -> "📍"
                            else -> "⚙️"
                        }
                        Text(iconEmoji, fontSize = 32.sp)
                    }

                    Text(
                        text = currentPermission.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = currentPermission.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Conditional Info/Warning Boxes
                    when (currentPermission) {
                        PermissionType.ACCESSIBILITY, PermissionType.USAGE_STATS -> {
                            InfoBoxV2("این دسترسی هیچ اطلاعات شخصی را جمع‌آوری نمی‌کند. فقط نام اپ‌های فعال را می‌بیند.")
                        }

                        PermissionType.DEVICE_ADMIN -> {
                            WarningBoxV2("توجه: این دسترسی فقط با رمز والدین قابل لغو است.")
                        }

                        PermissionType.VPN -> {
                            InfoBoxV2("این VPN هیچ داده‌ای به اینترنت ارسال نمی‌کند و فقط برای فیلتر داخلی استفاده می‌شود.")
                        }

                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Guide steps mapped from PermissionType.instruction
                    currentPermission.instruction.forEachIndexed { index, step ->
                        TutorialStepV2(stepNumber = index + 1, instruction = step)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// --- EXTRACTED COMPONENTS ---

@Composable
fun PermissionHeaderV2(currentPage: Int, totalPages: Int) {
    val colors = LocalCustomColors.current
    val headerGradient = Brush.linearGradient(listOf(colors.primary, colors.primaryVariant))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = headerGradient,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(top = 40.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        Brush.linearGradient(listOf(colors.yellow, colors.orange)),
                        RoundedCornerShape(14.dp)
                    )
                    .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = colors.yellow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    AppIcons.ShieldCheck,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    "مهربان",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(
                    "راه‌اندازی دسترسی‌ها",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }

        // Progress Bar Wrapper
        val progress by animateFloatAsState(
            targetValue = currentPage.toFloat() / totalPages.toFloat(),
            animationSpec = tween(500), label = "progress"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(6.dp)
                        .background(colors.yellow, RoundedCornerShape(3.dp))
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "$currentPage / $totalPages",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoBoxV2(text: String) {
    val colors = LocalCustomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.blue.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .border(1.dp, colors.blue.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            AppIcons.Info,
            contentDescription = null,
            tint = colors.blue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, color = colors.textPrimary, lineHeight = 18.sp)
    }
}

@Composable
fun WarningBoxV2(text: String) {
    val colors = LocalCustomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.orangeLight, RoundedCornerShape(10.dp))
            .border(1.dp, colors.orange.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("⚠️", fontSize = 18.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text,
            fontSize = 12.sp,
            color = Color(0xFFE65100),
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun TutorialStepV2(stepNumber: Int, instruction: String) {
    val colors = LocalCustomColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(colors.primary, colors.primaryVariant)),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stepNumber.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = instruction,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun PermissionFooterV2(buttonText: String, onGrantClick: () -> Unit) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp) // Shadow must be applied before background for standard elevation
            .background(colors.surface)
            .padding(20.dp)
    ) {
        Button(
            onClick = onGrantClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = colors.primary.copy(alpha = 0.4f)
                ),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
        ) {
            Text(
                text = buttonText,
                color = colors.textOnPrimaryVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Permission Slider Light", locale = "fa")
@Composable
fun PermissionSliderPreviewLight() {
    // Mocked PermissionType instructions just for the preview rendering
    val mockInstruction =
        listOf("به تنظیمات بروید", "بخش دسترسی‌ها را انتخاب کنید", "مهربان را فعال کنید")

    val mockPermissions = listOf(
        PermissionType.USAGE_STATS.apply { /* Mock logic */ },
        PermissionType.DEVICE_ADMIN,
        PermissionType.ACCESSIBILITY
    )

    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        PermissionSliderContent(
            permissions = mockPermissions,
            onGrantClick = {},
            onFinishSetup = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Permission Slider Dark", locale = "fa",
    showSystemUi = true, device = "spec:parent=pixel_5,navigation=buttons"
)
@Composable
fun PermissionSliderPreviewDark() {
    val mockPermissions = listOf(PermissionType.DEVICE_ADMIN)

    ParentControlTheme(themeMode = AppTheme.DARK) {
        PermissionSliderContent(
            permissions = mockPermissions,
            onGrantClick = {},
            onFinishSetup = {}
        )
    }
}