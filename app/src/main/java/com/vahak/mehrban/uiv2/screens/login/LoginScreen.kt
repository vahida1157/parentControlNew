package com.vahak.mehrban.uiv2.screens.login

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.util.AppSignatureHelper
import com.vahak.mehrban.presentation.login.LoginEffect
import com.vahak.mehrban.presentation.login.LoginEvent
import com.vahak.mehrban.presentation.login.LoginState
import com.vahak.mehrban.presentation.login.LoginViewModel
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.uiv2.components.AppBackground
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

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

    val goldGradient = Brush.linearGradient(
        colors = listOf(colors.yellow, colors.orange)
    )

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

    AppSignatureHelper(LocalContext.current).getAppSignatures()

    AppBackground(
        patternAlpha = 0.06f,
        patternScale = 2.5f,
        patternRepeatScale = 0.5f
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
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
                        painter = AppIcons.LockBadge,
                        contentDescription = "App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.login_subtitle),
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
                            text = stringResource(R.string.login_phone_label),
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
                                Box(
                                    modifier = Modifier
                                        .height(56.dp)
                                        .background(colors.cardInnerBG, RoundedCornerShape(10.dp))
                                        .border(2.dp, colors.divider, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.login_phone_prefix),
                                        color = colors.primary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }

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
                                            text = stringResource(R.string.login_phone_hint),
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

                        Spacer(modifier = Modifier.height(16.dp))

                        // Privacy Policy Checkbox
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onEvent(LoginEvent.PrivacyAcceptedChanged(!state.isPrivacyAccepted)) }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = state.isPrivacyAccepted,
                                onCheckedChange = { onEvent(LoginEvent.PrivacyAcceptedChanged(it)) },
                                colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                            )

                            val annotatedString = buildAnnotatedString {
                                append(stringResource(R.string.login_privacy_prefix))
                                withStyle(
                                    style = SpanStyle(
                                        color = colors.primary,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(stringResource(R.string.login_privacy_link))
                                }
                                append(stringResource(R.string.login_privacy_suffix))
                            }

                            Text(
                                text = annotatedString,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clickable { onEvent(LoginEvent.ShowPrivacyDialog(true)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                            enabled = !state.isLoading && state.phoneNumber.length == 10 && state.isPrivacyAccepted
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (!state.isLoading && state.phoneNumber.length == 10 && state.isPrivacyAccepted)
                                            Brush.linearGradient(
                                                listOf(
                                                    colors.primary,
                                                    colors.primaryVariant
                                                )
                                            )
                                        else
                                            Brush.linearGradient(
                                                listOf(
                                                    colors.divider,
                                                    colors.divider
                                                )
                                            )
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
                                        text = stringResource(R.string.login_button_text),
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

    // Privacy Policy Dialog
    if (state.showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(LoginEvent.ShowPrivacyDialog(false)) },
            containerColor = colors.surface,
            title = {
                Text(
                    text = stringResource(R.string.login_privacy_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = stringResource(R.string.login_privacy_dialog_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        lineHeight = 24.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onEvent(LoginEvent.PrivacyAcceptedChanged(true))
                    onEvent(LoginEvent.ShowPrivacyDialog(false))
                }) {
                    Text(stringResource(R.string.login_privacy_accept), color = colors.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(LoginEvent.ShowPrivacyDialog(false)) }) {
                    Text(stringResource(R.string.cancel), color = colors.textHint)
                }
            }
        )
    }
}

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