package com.vahak.parentcontroll.uiv2.screens.login

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.login.OtpEffect
import com.vahak.parentcontroll.presentation.login.OtpEvent
import com.vahak.parentcontroll.presentation.login.OtpState
import com.vahak.parentcontroll.presentation.login.OtpViewModel
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme

@Composable
fun OtpScreen(
    phoneNumber: String,
    viewModel: OtpViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onVerifyClick: () -> Unit,
    onNavigateToPasswordSetup: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OtpEffect.NavigateToDashboard -> onVerifyClick()
                is OtpEffect.NavigateToPasswordSetup -> onNavigateToPasswordSetup()
                is OtpEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    OtpScreenContent(
        state = state,
        phoneNumber = phoneNumber,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick
    )
}

@Composable
fun OtpScreenContent(
    state: OtpState,
    phoneNumber: String,
    onEvent: (OtpEvent) -> Unit,
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val otpLength = 4

    // Background Teal Gradient (Constant for Light/Dark)
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(colors.primary, colors.primaryVariant, Color(0xFF0A4F46))
    )

    // Button & Active element Gold Gradient
    val goldGradient = Brush.linearGradient(
        colors = listOf(colors.yellow, colors.orange)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Absolute top back button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = AppIcons.Back,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Foreground Content (Directly on gradient, no card)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gold Logo Mini
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(goldGradient, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = AppIcons.LockBadge,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "تأیید شماره تلفن",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "کد ارسال شده به ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.yellow,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = " را وارد کنید",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = colors.redLight, // Better visibility on dark teal
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // OTP Input Field Wrapper
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BasicTextField(
                    value = state.otpCode,
                    onValueChange = { newValue ->
                        val filtered = newValue.filter { it.isDigit() }
                        if (filtered.length <= otpLength) {
                            onEvent(OtpEvent.OtpChanged(filtered))
                            if (filtered.length == otpLength && !state.isVerifying) {
                                onEvent(OtpEvent.VerifyClicked(phoneNumber))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(Color.Transparent),
                    decorationBox = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            repeat(otpLength) { index ->
                                val char = state.otpCode.getOrNull(index)?.toString() ?: ""
                                val isFilled = state.otpCode.length > index
                                val isFocused = state.otpCode.length == index
                                val isError = state.errorMessage != null

                                // HTML Logic: filled/focused turns gold, else white/translucent
                                val borderColor = when {
                                    isError -> colors.red
                                    isFocused || isFilled -> colors.yellow
                                    else -> Color.White.copy(alpha = 0.3f)
                                }
                                val bgColor = when {
                                    isFocused || isFilled -> colors.yellow.copy(alpha = 0.2f)
                                    else -> Color.White.copy(alpha = 0.1f)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(52.dp, 60.dp)
                                        .background(bgColor, RoundedCornerShape(12.dp))
                                        .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = char,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Timer & Resend
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val seconds = state.timerSeconds.toString()

                if (!state.canResend) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ارسال مجدد تا ",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$seconds ثانیه",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.yellow
                        )
                    }
                } else {
                    Text(
                        text = "ارسال مجدد کد",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.yellow,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            onEvent(OtpEvent.ResendClicked(phoneNumber))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            val isEnabled = state.otpCode.length == otpLength && !state.isVerifying
            Button(
                onClick = { onEvent(OtpEvent.VerifyClicked(phoneNumber)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = colors.yellow.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = colors.divider.copy(alpha = 0.3f)
                ),
                contentPadding = PaddingValues(),
                enabled = isEnabled
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isEnabled) goldGradient
                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isVerifying) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "تأیید کد",
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

// 3. SAFE PREVIEWS
@Preview(showBackground = true, locale = "fa", name = "1. OTP Light Mode")
@Composable
fun OtpScreenPreview() {
    ParentControlTheme {
        OtpScreenContent(
            state = OtpState(otpCode = "12", timerSeconds = 60),
            phoneNumber = "09123456789",
            onEvent = {},
            onBackClick = {}
        )
    }
}