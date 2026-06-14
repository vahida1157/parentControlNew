package com.vahak.mehrban.ui.screens.permissions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.receiver.SecurityAdminReceiver
import com.vahak.mehrban.core.util.PermissionChecker
import com.vahak.mehrban.core.util.PermissionType
import com.vahak.mehrban.presentation.permissions.PermissionSliderEffect
import com.vahak.mehrban.presentation.permissions.PermissionSliderEvent
import com.vahak.mehrban.presentation.permissions.PermissionSliderViewModel
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme

@Composable
fun PermissionSliderScreen(
    viewModel: PermissionSliderViewModel = hiltViewModel(),
    onNavigateToFeature: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val deviceAdminExplanation = stringResource(R.string.permission_device_admin_explanation)
    val helpMessage = stringResource(R.string.help_section_under_construction)

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
                                    deviceAdminExplanation
                                )
                            }

                            else -> { /* Standard Settings intents don't need extra data */
                            }
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
            onFinishSetup = { viewModel.onEvent(PermissionSliderEvent.SetupFinished) },
            onHelpClick = {
                Toast.makeText(
                    context,
                    helpMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
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
    onHelpClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val pagerState = rememberPagerState(pageCount = { permissions.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val grantAccessText = stringResource(R.string.permission_grant_access)
    val finalConfirmText = stringResource(R.string.permission_final_confirm)
    val underConstructionText = stringResource(R.string.help_section_under_construction)

    LaunchedEffect(checkTrigger) {
        if (checkTrigger > 0 && permissionToCheck != null) {
            if (PermissionChecker.hasPermission(context, permissionToCheck)) {
                val currentPage = pagerState.currentPage
                if (currentPage < permissions.lastIndex) {
                    pagerState.animateScrollToPage(currentPage + 1)
                } else {
                    onFinishSetup()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) { page ->
                val currentPermission = permissions[page]
                val isLastPage = page == permissions.lastIndex

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    // --- STEP DOTS ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(permissions.size) { index ->
                            val isActive = index == page
                            val dotColor by animateColorAsState(
                                targetValue = if (isActive) colors.primary else colors.divider,
                                animationSpec = tween(durationMillis = 300), label = "dotColor"
                            )

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }

                    // --- CONTENT WRAPPER ---
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(currentPermission.titleRes),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = stringResource(currentPermission.descRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                currentPermission.instructionResIds.forEach { resId ->
                                    Text(
                                        text = stringResource(resId),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // --- FOOTER WRAPPER ---
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { onGrantClick(currentPermission) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                        ) {
                            Text(
                                text = if (isLastPage) finalConfirmText else grantAccessText,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Permission Slider", locale = "fa")
@Composable
fun PermissionSliderPreview() {
    ParentControlTheme {
        PermissionSliderContent(
            permissions = listOf(
                PermissionType.USAGE_STATS,
                PermissionType.OVERLAY,
                PermissionType.LOCATION
            ),
            onGrantClick = {},
            onFinishSetup = {},
            onHelpClick = {}
        )
    }
}