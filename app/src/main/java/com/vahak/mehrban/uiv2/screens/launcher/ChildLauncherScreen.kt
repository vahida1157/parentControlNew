package com.vahak.mehrban.uiv2.screens.launcher

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.core.data.local.entity.Gender
import com.vahak.mehrban.core.util.AppInfo
import com.vahak.mehrban.presentation.launcher.LauncherEffectV2
import com.vahak.mehrban.presentation.launcher.LauncherEventV2
import com.vahak.mehrban.presentation.launcher.LauncherStateV2
import com.vahak.mehrban.presentation.launcher.LauncherViewModelV2
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChildLauncherScreen(
    viewModel: LauncherViewModelV2 = hiltViewModel(),
    onExitLauncherClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LauncherEffectV2.RequestExit -> {
                    Toast.makeText(context, "خروج از محیط امن", Toast.LENGTH_SHORT).show()
                    onExitLauncherClick()
                }

                is LauncherEffectV2.ShowToast -> Toast.makeText(
                    context,
                    "${effect.icon} ${effect.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    ChildLauncherContentV2(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun ChildLauncherContentV2(
    state: LauncherStateV2,
    onEvent: (LauncherEventV2) -> Unit
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .background(Brush.linearGradient(listOf(colors.background, colors.surface)))
    ) {
        AnimatedBackgroundOrbs()

        Column(modifier = Modifier.fillMaxSize()) {
//            LauncherStatusBar()
            LauncherHeader(
                childName = state.childName,
                gender = state.gender,
                onExitClick = { onEvent(LauncherEventV2.ExitLauncherClicked) }
            )
            LauncherTimeCard(
                usageSeconds = state.usageSeconds,
                limitMins = state.timeLimitMins,
                isActive = state.isTimeLimitActive
            )

            Spacer(modifier = Modifier.height(16.dp))

            LauncherAppGrid(
                apps = state.installedApps,
                onAppClick = { onEvent(LauncherEventV2.AppClicked(it)) }
            )
        }

//        LauncherBottomDock(modifier = Modifier.align(Alignment.BottomCenter))

        if (state.showExitDialog) {
            LauncherPinDialog(
                enteredPin = state.enteredPin,
                isError = state.pinError,
                onDigitClick = { onEvent(LauncherEventV2.PinDigitEntered(it)) },
                onBackspaceClick = { onEvent(LauncherEventV2.PinBackspaceClicked) },
                onSubmitClick = { onEvent(LauncherEventV2.SubmitExitPin(state.enteredPin)) },
                onCancelClick = { onEvent(LauncherEventV2.DismissExitDialog) },
                onForgotPinClick = { onEvent(LauncherEventV2.ForgotPinClicked) },
            )
        }

        // 🚀 NEW: Security Question Dialog
        if (state.showRecoveryDialog) {
            LauncherRecoveryDialog(
                question = state.securityQuestion,
                answerInput = state.recoveryAnswerInput,
                isError = state.recoveryError,
                onAnswerChange = { onEvent(LauncherEventV2.RecoveryAnswerChanged(it)) },
                onSubmitClick = { onEvent(LauncherEventV2.SubmitRecoveryAnswer) },
                onCancelClick = { onEvent(LauncherEventV2.DismissRecoveryDialog) },
            )
        }
    }
}

@Composable
fun AnimatedBackgroundOrbs() {
    val colors = LocalCustomColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    val offsetY1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb1"
    )
    val offsetY2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb2"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset { IntOffset(100, offsetY1.toInt() - 100) }
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {}
        Box(
            modifier = Modifier
                .offset { IntOffset(-100, offsetY2.toInt() + 500) }
                .size(350.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            colors.orange.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {}
    }
}

@Composable
fun LauncherStatusBar() {
    val colors = LocalCustomColors.current
    var currentTime by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            kotlinx.coroutines.delay(1000)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = currentTime, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🛡️", fontSize = 14.sp)
            Text("📶", fontSize = 14.sp)
            Text("🔋", fontSize = 14.sp)
        }
    }
}

@Composable
fun LauncherHeader(childName: String, gender: Gender, onExitClick: () -> Unit) {
    val colors = LocalCustomColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "rotate"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                colors.primary,
                                colors.orange,
                                colors.yellow,
                                colors.primary
                            )
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.surface, RoundedCornerShape(18.dp))
                    .border(3.dp, colors.surface, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (gender == Gender.BOY) "👦" else "👧", fontSize = 32.sp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-2).dp, y = 2.dp)
                    .size(14.dp)
                    .background(colors.green, CircleShape)
                    .border(2.dp, colors.surface, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "سلام،",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
            Text(
                childName.ifEmpty { "فرزند" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(colors.surface.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(14.dp))
                .clickable { onExitClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("🔓", fontSize = 20.sp)
        }
    }
}

@Composable
fun LauncherTimeCard(usageSeconds: Int, limitMins: Int, isActive: Boolean) {
    val colors = LocalCustomColors.current

    val usageHours = usageSeconds / 3600
    val usageMins = (usageSeconds % 3600) / 60

    val totalLimitSeconds = if (isActive && limitMins > 0) limitMins * 60 else 1
    val progress =
        if (isActive && limitMins > 0) (usageSeconds.toFloat() / totalLimitSeconds).coerceIn(
            0f,
            1f
        ) else 0f

    val remainSeconds = maxOf(0, (limitMins * 60) - usageSeconds)
    val remainHours = remainSeconds / 3600
    val remainMins = (remainSeconds % 3600) / 60

    // PRO FIX: Dynamic solid color replacing the confusing gradient
    val barColor = when {
        !isActive || limitMins == 0 -> colors.green
        progress > 0.9f -> colors.red
        progress > 0.75f -> colors.orange
        else -> colors.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .background(colors.surface.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "⏱️ زمان امروز",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    String.format("%d:%02d", usageHours, usageMins),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.primary
                )
                if (isActive && limitMins > 0) {
                    Text(
                        " از ",
                        fontSize = 12.sp,
                        color = colors.textHint,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        String.format("%d:%02d", limitMins / 60, limitMins % 60),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                } else {
                    Text(
                        " (نامحدود)",
                        fontSize = 12.sp,
                        color = colors.textHint,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // PRO FIX: LTR Layout with Dynamic Solid Color
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(colors.divider, RoundedCornerShape(6.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isActive && limitMins > 0) progress else 0f)
                        .fillMaxHeight()
                        .background(barColor, RoundedCornerShape(6.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isActive && limitMins > 0) {
            val remainText = buildString {
                if (remainHours > 0) append("$remainHours ساعت و ")
                append("$remainMins دقیقه باقی‌مانده")
            }
            Text(
                text = remainText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (progress >= 1f) colors.red else colors.textSecondary
            )
        }
    }
}

@Composable
fun LauncherAppGrid(apps: List<AppInfo>, onAppClick: (String) -> Unit) {
    val colors = LocalCustomColors.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        items(apps) { app ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onAppClick(app.packageName) }
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(colors.surface.copy(alpha = 0.8f), RoundedCornerShape(18.dp))
                        .shadow(
                            4.dp,
                            RoundedCornerShape(18.dp),
                            spotColor = colors.textPrimary.copy(alpha = 0.15f)
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val safeBitmap = remember(app.icon) {
                        try {
                            app.icon.toBitmap(144, 144).asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (safeBitmap != null) {
                        Image(
                            bitmap = safeBitmap,
                            contentDescription = app.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("📱", fontSize = 28.sp)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = app.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LauncherBottomDock(modifier: Modifier = Modifier) {
    val colors = LocalCustomColors.current

    Row(
        modifier = modifier
            .padding(start = 18.dp, end = 18.dp, bottom = 24.dp)
            .fillMaxWidth()
            .background(colors.surface.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DockItem("📞", "تماس", colors.green)
        DockItem("📷", "دوربین", colors.primaryVariant) // Replacing purple with primaryVariant
        DockItem("🖼️", "گالری", colors.red)
        DockItem("🏠", "خانه", colors.primary)
    }
}

@Composable
fun DockItem(icon: String, label: String, bgTint: Color) {
    val colors = LocalCustomColors.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(bgTint, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
    }
}

@Composable
fun LauncherPinDialog(
    enteredPin: String,
    isError: Boolean,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onCancelClick: () -> Unit,
    onForgotPinClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    val offsetX by animateIntAsState(
        targetValue = if (isError) 15 else 0,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "shake"
    )

    Dialog(onDismissRequest = onCancelClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(26.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(colors.red.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔐", fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "رمز والد",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )
            Text(
                "برای خروج از حالت لانچر، رمز والد را وارد کنید",
                fontSize = 12.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
            )

            // 🚀 FIXED: Custom Text-Based Input Field (No system keyboard pop-up, no fake dots)
            val x = if (isError) (-offsetX).dp else offsetX.dp
            Box(
                modifier = Modifier
                    .offset(x = x)
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(colors.cardInnerBG, RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        when {
                            isError -> colors.red
                            enteredPin.isNotEmpty() -> colors.primary
                            else -> colors.divider
                        },
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (enteredPin.isEmpty()) {
                    Text(
                        text = "رمز عبور (۴ تا ۸ رقم)",
                        color = colors.textHint,
                        fontSize = 14.sp
                    )
                } else {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            // Converts "1234" into "●  ●  ●  ●"
                            text = enteredPin.map { "●" }.joinToString("  "),
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (isError) "رمز اشتباه است. دوباره تلاش کنید." else "",
                color = colors.red,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.height(16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            val buttons = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "✓", "0", "⌫")

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    items(buttons) { btn ->
                        val isActionBtn = btn == "✓" || btn == "⌫"
                        val isPinLengthValid = enteredPin.length in 4..8

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(
                                    if (isActionBtn) Color.Transparent else colors.cardInnerBG,
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isActionBtn) Color.Transparent else colors.divider,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    when (btn) {
                                        "✓" -> if (isPinLengthValid) onSubmitClick()
                                        "⌫" -> onBackspaceClick()
                                        else -> if (enteredPin.length < 8) onDigitClick(btn)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = btn,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (btn) {
                                    "⌫" -> colors.red
                                    "✓" -> if (isPinLengthValid) colors.primary else colors.textHint
                                    else -> colors.textPrimary
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onForgotPinClick, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "رمز عبور را فراموش کرده‌اید؟",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                Text("انصراف", color = colors.textHint, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// 🚀 NEW COMPOSABLE: Recovery Dialog
@Composable
fun LauncherRecoveryDialog(
    question: String,
    answerInput: String,
    isError: Boolean,
    onAnswerChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Dialog(onDismissRequest = onCancelClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(26.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(colors.orangeLight.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🛡️", fontSize = 30.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "بازیابی رمز عبور",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "سوال امنیتی:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Text(
                question,
                fontSize = 14.sp,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                textAlign = TextAlign.Start
            )

            androidx.compose.material3.OutlinedTextField(
                value = answerInput,
                onValueChange = onAnswerChange,
                placeholder = { Text("پاسخ شما", color = colors.textHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    unfocusedContainerColor = colors.cardInnerBG,
                    focusedContainerColor = colors.surface,
                    unfocusedBorderColor = if (isError) colors.red else colors.divider,
                    focusedBorderColor = if (isError) colors.red else colors.primary,
                )
            )

            if (isError) {
                Text(
                    "پاسخ اشتباه است.",
                    color = colors.red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            androidx.compose.material3.Button(
                onClick = onSubmitClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    disabledContainerColor = colors.divider
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = answerInput.isNotBlank()
            ) {
                Text(
                    "بررسی و خروج",
                    color = colors.textOnPrimaryVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                Text("انصراف", color = colors.textHint, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ----------------------------------------------------------------------------
// PREVIEWS
// ----------------------------------------------------------------------------
@Preview(
    showBackground = true, name = "1. Launcher V2", locale = "fa", showSystemUi = true,
    device = "id:pixel_5"
)
@Composable
fun ChildLauncherPreviewLightV2() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        ChildLauncherContentV2(
            state = LauncherStateV2(
                childName = "محمدمهدی",
                gender = Gender.BOY,
                isTimeLimitActive = true,
                timeLimitMins = 120,
                usageSeconds = 3600 // 1 hour used
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Exit Dialog V2", locale = "fa")
@Composable
fun ChildLauncherDialogPreviewV2() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        ChildLauncherContentV2(
            state = LauncherStateV2(
                childName = "محمدمهدی",
                gender = Gender.BOY,
                showExitDialog = true,
                enteredPin = "12"
            ),
            onEvent = {}
        )
    }
}