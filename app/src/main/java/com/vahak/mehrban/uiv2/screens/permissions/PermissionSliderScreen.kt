package com.vahak.mehrban.uiv2.screens.permissions

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.receiver.SecurityAdminReceiver
import com.vahak.mehrban.core.util.PermissionType
import com.vahak.mehrban.presentation.permissions.PermissionSliderEffect
import com.vahak.mehrban.presentation.permissions.PermissionSliderEvent
import com.vahak.mehrban.presentation.permissions.PermissionSliderViewModel
import com.vahak.mehrban.uiv2.components.header.MehrbanPermissionHeader
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import timber.log.Timber

@Composable
fun PermissionSliderScreen(
    viewModel: PermissionSliderViewModel = hiltViewModel(), onNavigateToFeature: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val deviceAdminExplanation = stringResource(R.string.permission_device_admin_explanation)

    var checkTrigger by remember { mutableIntStateOf(0) }
    var currentPermissionToCheck by remember { mutableStateOf<PermissionType?>(null) }

    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        checkTrigger++
    }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        Timber.d("Notification Permission Result -> isGranted: $isGranted")

        if (!isGranted) {
            val activity = context.findActivity()
            Timber.d("Unwrapped Activity: ${activity?.localClassName ?: "NULL"}")

            if (activity != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                val shouldShowRationale =
                    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        POST_NOTIFICATIONS
                    )
                Timber.d("shouldShowRationale flag: $shouldShowRationale")

                if (!shouldShowRationale) {
                    Timber.w("Permission permanently denied! Routing to OS Settings.")
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    settingsLauncher.launch(intent)
                    return@rememberLauncherForActivityResult
                } else {
                    Timber.d("User denied once. OS will still allow the popup next time.")
                }
            } else {
                Timber.e("Could not check rationale. Activity is null or SDK < 33.")
            }
        }

        checkTrigger++
    }

    LaunchedEffect(checkTrigger) {
        if (checkTrigger > 0) {
            viewModel.onEvent(PermissionSliderEvent.CheckPermissions(context))
        }
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

                    if (effect.permission == PermissionType.NOTIFICATIONS) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            runtimePermissionLauncher.launch(POST_NOTIFICATIONS)
                        } else {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            settingsLauncher.launch(intent)
                        }
                        return@collect
                    }

                    val intent = Intent(effect.action).apply {
                        when (effect.permission) {
                            PermissionType.OVERLAY -> {
                                data = "package:${context.packageName}".toUri()
                            }

                            PermissionType.DEVICE_ADMIN -> {
                                val adminComponent =
                                    ComponentName(context, SecurityAdminReceiver::class.java)
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    deviceAdminExplanation
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
            onGrantClick = { permission ->
                viewModel.onEvent(PermissionSliderEvent.GrantClicked(permission))
            },
            onSkipClick = { viewModel.onEvent(PermissionSliderEvent.SkipClicked) },
            onFinishSetup = { viewModel.onEvent(PermissionSliderEvent.SetupFinished) })
    }
}

@Composable
fun PermissionSliderContent(
    permissions: List<PermissionType>,
    onGrantClick: (PermissionType) -> Unit,
    onSkipClick: () -> Unit,
    onFinishSetup: () -> Unit,
) {
    val colors = LocalCustomColors.current
    val pagerState = rememberPagerState(pageCount = { permissions.size })
    val context = LocalContext.current
    val appName = remember {
        context.applicationInfo.loadLabel(context.packageManager).toString()
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(), containerColor = colors.background, bottomBar = {
            val currentPerm = permissions.getOrNull(pagerState.currentPage)
            val canSkip = currentPerm == PermissionType.NOTIFICATIONS

            PermissionFooterV2(
                buttonText = stringResource(R.string.permission_grant_access),
                onGrantClick = { onGrantClick(permissions[pagerState.currentPage]) },
                showSkip = canSkip,
                onSkipClick = onSkipClick
            )
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MehrbanPermissionHeader(
                currentPage = pagerState.currentPage + 1, totalPages = permissions.size
            )

            HorizontalPager(
                state = pagerState, modifier = Modifier.fillMaxSize(), userScrollEnabled = false
            ) { page ->

                if (page >= permissions.size) {
                    onFinishSetup()
                    return@HorizontalPager
                }

                val currentPermission = permissions[page]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .size(72.dp)
                            .background(colors.cardInnerBG, RoundedCornerShape(20.dp))
                            .border(
                                2.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(20.dp)
                            ), contentAlignment = Alignment.Center
                    ) {
                        val iconEmoji = when (currentPermission) {
                            PermissionType.ACCESSIBILITY -> "👁️"
                            PermissionType.USAGE_STATS -> "📊"
                            PermissionType.DEVICE_ADMIN -> "🛡️"
                            PermissionType.VPN -> "🌐"
                            PermissionType.OVERLAY -> "📱"
                            PermissionType.LOCATION -> "📍"
                            PermissionType.NOTIFICATIONS -> "🔔"
                        }
                        Text(iconEmoji, fontSize = 32.sp)
                    }

                    Text(
                        text = stringResource(currentPermission.titleRes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = stringResource(currentPermission.descRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    when (currentPermission) {
                        PermissionType.ACCESSIBILITY, PermissionType.USAGE_STATS -> {
                            InfoBoxV2(stringResource(R.string.permission_info_accessibility_usage))
                        }

                        PermissionType.DEVICE_ADMIN -> {
                            WarningBoxV2(stringResource(R.string.permission_warning_device_admin))
                        }

                        PermissionType.VPN -> {
                            InfoBoxV2(stringResource(R.string.permission_info_vpn))
                        }

                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Guide steps mapped from resource IDs
                    currentPermission.instructionResIds.forEachIndexed { index, stepResId ->
                        val rawInstruction = stringResource(id = stepResId, appName)

                        TutorialStepWithIconV2(
                            stepNumber = index + 1,
                            instruction = rawInstruction
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// --- EXTRACTED COMPONENTS ---

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
            .padding(12.dp), verticalAlignment = Alignment.CenterVertically
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
fun TutorialStepWithIconV2(stepNumber: Int, instruction: String) {
    val colors = LocalCustomColors.current

    val inlineContentMap = mapOf(
        "app_icon" to InlineTextContent(
            Placeholder(
                width = 32.sp,
                height = 32.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher),
                contentDescription = "App Icon",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    )

    val annotatedInstruction = buildAnnotatedString {
        val parts = instruction.split("[ICON]")
        parts.forEachIndexed { index, part ->
            append(part)
            if (index < parts.size - 1) {
                appendInlineContent("app_icon", "[icon]")
                append(" ")
            }
        }
    }

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
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(colors.primary, colors.primaryVariant)),
                        RoundedCornerShape(12.dp)
                    ), contentAlignment = Alignment.Center
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
                text = annotatedInstruction,
                inlineContent = inlineContentMap,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun PermissionFooterV2(
    buttonText: String,
    onGrantClick: () -> Unit,
    showSkip: Boolean = false,
    onSkipClick: () -> Unit = {}
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp)
            .background(colors.surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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

        if (showSkip) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onSkipClick) {
                Text(
                    text = stringResource(R.string.skip_for_now),
                    color = colors.textHint,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Permission Slider Light", locale = "fa")
@Composable
fun PermissionSliderPreviewLight() {
    val mockPermissions = listOf(
        PermissionType.NOTIFICATIONS,
        PermissionType.USAGE_STATS,
        PermissionType.DEVICE_ADMIN,
        PermissionType.ACCESSIBILITY
    )

    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        PermissionSliderContent(
            permissions = mockPermissions,
            onGrantClick = {},
            onSkipClick = {},
            onFinishSetup = {})
    }
}

@Preview(
    showBackground = true,
    name = "2. Permission Slider Dark",
    locale = "fa",
    showSystemUi = true,
    device = "spec:parent=pixel_5,navigation=buttons"
)
@Composable
fun PermissionSliderPreviewDark() {
    val mockPermissions = listOf(PermissionType.DEVICE_ADMIN)

    ParentControlTheme(themeMode = AppTheme.DARK) {
        PermissionSliderContent(
            permissions = mockPermissions,
            onGrantClick = {},
            onSkipClick = {},
            onFinishSetup = {})
    }
}