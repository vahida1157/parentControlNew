package com.vahak.parentcontroll.ui.screens.login

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.login.OtpEffect
import com.vahak.parentcontroll.presentation.login.OtpEvent
import com.vahak.parentcontroll.presentation.login.OtpViewModel
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun OtpScreen(
    phoneNumber: String,
    viewModel: OtpViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onVerifyClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val otpLength = 4

    // Handle One-Off Effects from ViewModel
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OtpEffect.NavigateToDashboard -> onVerifyClick()
                is OtpEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val primaryGradient = Brush.linearGradient(
        colors = listOf(colors.primary, colors.secondary)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.background, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = AppIcons.Back,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(primaryGradient, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = AppIcons.LockBadge,
                            contentDescription = null,
                            tint = colors.surface,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(40.dp))
                }

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "تایید شماره موبایل",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "کد ۴ رقمی پیامک شده به شماره\n$phoneNumber را وارد کنید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Error Message Display
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = colors.red,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 15.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(35.dp))
                }

                // --- OTP LTR INPUT BOXES ---
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    BasicTextField(
                        value = state.otpCode,
                        onValueChange = { viewModel.onEvent(OtpEvent.OtpChanged(it)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        cursorBrush = SolidColor(colors.primary),
                        decorationBox = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(
                                    12.dp,
                                    Alignment.CenterHorizontally
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                repeat(otpLength) { index ->
                                    val isFocused = state.otpCode.length == index
                                    val char = state.otpCode.getOrNull(index)?.toString() ?: ""

                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(
                                                color = if (isFocused) colors.surface else colors.background,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                            .border(
                                                width = 2.dp,
                                                color = if (state.errorMessage != null) colors.red
                                                else if (isFocused) colors.primary
                                                else colors.divider,
                                                shape = RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = char,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "ارسال مجدد کد (01:59)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.onEvent(OtpEvent.ResendClicked) }
                )

                Spacer(modifier = Modifier.height(30.dp))

                val isEnabled = state.otpCode.length == otpLength && !state.isVerifying
                Button(
                    onClick = { viewModel.onEvent(OtpEvent.VerifyClicked(phoneNumber)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = colors.backgroundButtonDisable
                    ),
                    contentPadding = PaddingValues(),
                    enabled = isEnabled
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isEnabled) primaryGradient
                                else Brush.linearGradient(
                                    listOf(
                                        colors.backgroundButtonDisable,
                                        colors.backgroundButtonDisable
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.isVerifying) "درحال بررسی..." else "تایید و ورود",
                            color = colors.surface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "1. OTP Light Mode", locale = "fa")
@Composable
fun OtpScreenLightPreview() {
    ParentControlTheme(darkTheme = false) {
        OtpScreen(
            phoneNumber = "09123456789",
            onBackClick = {},
            onVerifyClick = {}
        )
    }
}

@Preview(showBackground = true, name = "2. OTP Dark Mode", locale = "fa")
@Composable
fun OtpScreenDarkPreview() {
    ParentControlTheme(darkTheme = true) {
        OtpScreen(
            phoneNumber = "09123456789",
            onBackClick = {},
            onVerifyClick = {}
        )
    }
}