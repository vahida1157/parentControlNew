package com.vahak.parentcontroll.uiv2.screens.login

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.login.LoginEffect
import com.vahak.parentcontroll.presentation.login.LoginEvent
import com.vahak.parentcontroll.presentation.login.LoginState
import com.vahak.parentcontroll.presentation.login.LoginViewModel
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToOtp: (String, Int) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginEffect.NavigateToOtp -> onNavigateToOtp(effect.phone, effect.ttl)
            }
        }
    }

    LoginScreenContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun LoginScreenContent(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    // Background Teal Gradient (Constant for both Light/Dark as per HTML design)
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(colors.primary, colors.primaryVariant, Color(0xFF0A4F46))
    )

    // Logo Gold Gradient
    val goldGradient = Brush.linearGradient(
        colors = listOf(colors.yellow, colors.orange)
    )

    // Floating Animation for the logo
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = { it }),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Gold Logo
            Box(
                modifier = Modifier
                    .offset(offset = { IntOffset(0, floatOffset.toInt()) })
                    .size(120.dp)
                    .background(goldGradient, CircleShape)
                    .shadow(16.dp, CircleShape, spotColor = colors.yellow.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = AppIcons.LockBadge, // Replace with your Shield/Lock icon
                    contentDescription = "App Logo",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Title & Subtitle (Always white due to dark teal background)
            Text(
                text = "خانواده مدرن",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Black,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "محافظ خانواده شما 💚",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Input Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage,
                            color = colors.red,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Text(
                        text = "شماره تلفن همراه",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Phone Input Structure (LTR Forced)
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // +98 Prefix Box
                            Box(
                                modifier = Modifier
                                    .height(56.dp)
                                    .background(colors.cardInnerBG, RoundedCornerShape(10.dp))
                                    .border(2.dp, colors.divider, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+98",
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            // Phone Number Input
                            OutlinedTextField(
                                value = state.phoneNumber,
                                onValueChange = {
                                    val filtered = it.filter { char -> char.isDigit() }
                                    if (filtered.length <= 10) onEvent(
                                        LoginEvent.PhoneChanged(
                                            filtered
                                        )
                                    )
                                },
                                placeholder = {
                                    Text(
                                        text = "912 345 6789",
                                        color = colors.textHint,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Start,
                                    letterSpacing = 2.sp
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    unfocusedContainerColor = colors.cardInnerBG,
                                    focusedContainerColor = colors.surface,
                                    unfocusedBorderColor = colors.divider,
                                    focusedBorderColor = colors.primary,
                                    cursorColor = colors.primary,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submit Button
                    Button(
                        onClick = { onEvent(LoginEvent.SubmitClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                8.dp,
                                RoundedCornerShape(12.dp),
                                spotColor = colors.primary.copy(alpha = 0.3f)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = colors.divider
                        ),
                        contentPadding = PaddingValues(),
                        enabled = !state.isLoading && state.phoneNumber.length == 10
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (!state.isLoading && state.phoneNumber.length == 10)
                                        Brush.linearGradient(
                                            listOf(
                                                colors.primary,
                                                colors.primaryVariant
                                            )
                                        )
                                    else
                                        Brush.linearGradient(listOf(colors.divider, colors.divider))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = colors.textOnPrimaryVariant,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "دریافت کد تأیید",
                                    color = colors.textOnPrimaryVariant,
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
}

// 3. SAFE PREVIEWS
@Preview(showBackground = true, locale = "fa", name = "1. Normal State")
@Composable
fun LoginScreenPreview() {
    ParentControlTheme {
        LoginScreenContent(
            state = LoginState(phoneNumber = ""),
            onEvent = {}
        )
    }
}