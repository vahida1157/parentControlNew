package com.vahak.mehrban.uiv2.screens.login

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.vahak.mehrban.R
import com.vahak.mehrban.domain.error.AuthError
import com.vahak.mehrban.presentation.login.OtpEffect
import com.vahak.mehrban.presentation.login.OtpEvent
import com.vahak.mehrban.presentation.login.OtpState
import com.vahak.mehrban.presentation.login.OtpViewModel
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.uiv2.components.AppBackground
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

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

    val otpResentSuccessMessage = stringResource(R.string.otp_resend_success)

    DisposableEffect(context) {
        val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
                    val extras = intent.extras
                    // 🚀 THE FIX: Type-safe extraction based on the user's Android version
                    val status: Status? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(SmsRetriever.EXTRA_STATUS, Status::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(SmsRetriever.EXTRA_STATUS)
                        }

                    when (status?.statusCode) {
                        CommonStatusCodes.SUCCESS -> {
                            val message = extras?.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                            val code = message?.let { Regex("\\d{4}").find(it)?.value }
                            if (code != null) {
                                viewModel.onEvent(OtpEvent.OtpChanged(code))
                                viewModel.onEvent(OtpEvent.VerifyClicked(phoneNumber))
                            }
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        ContextCompat.registerReceiver(
            context, smsReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED
        )

        SmsRetriever.getClient(context).startSmsRetriever()

        onDispose {
            context.unregisterReceiver(smsReceiver)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is OtpEffect.NavigateToDashboard -> onVerifyClick()
                is OtpEffect.NavigateToPasswordSetup -> onNavigateToPasswordSetup()
                // 🚀 Matches the Clean Architecture Effect we made previously
                is OtpEffect.ShowResendSuccessToast -> {
                    Toast.makeText(
                        context, otpResentSuccessMessage, Toast.LENGTH_SHORT
                    ).show()
                }
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
    state: OtpState, phoneNumber: String, onEvent: (OtpEvent) -> Unit, onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val otpLength = 4

    val goldGradient = Brush.linearGradient(
        colors = listOf(colors.yellow, colors.orange)
    )

    AppBackground(
        patternAlpha = 0.06f, patternScale = 2.5f, patternRepeatScale = 0.5f
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = 24.dp, vertical = 80.dp
                    ), contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                        text = stringResource(R.string.otp_title),
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
                            text = stringResource(R.string.otp_subtitle_prefix),
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
                            text = stringResource(R.string.otp_subtitle_suffix),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 🚀 Translate Domain Error State
                    if (state.authError != null) {
                        val errorMessage = when (state.authError) {
                            AuthError.NETWORK_UNAVAILABLE -> stringResource(R.string.error_network_connection)
                            AuthError.SERVER_REJECTION -> stringResource(R.string.error_server_communication)
                            AuthError.WRONG_VERIFICATION_CODE -> stringResource(R.string.error_wrong_verification_code)
                            AuthError.UNKNOWN -> stringResource(R.string.error_unknown)
                        }

                        Text(
                            text = errorMessage,
                            color = colors.redLight,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

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
                            textStyle = TextStyle(color = Color.Transparent),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(
                                        12.dp, Alignment.CenterHorizontally
                                    ), modifier = Modifier.fillMaxWidth()
                                ) {
                                    repeat(otpLength) { index ->
                                        val char = state.otpCode.getOrNull(index)?.toString() ?: ""
                                        val isFilled = state.otpCode.length > index
                                        val isFocused = state.otpCode.length == index
                                        val isError = state.authError != null // 🚀 Check enum state

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
                                                .border(
                                                    2.dp, borderColor, RoundedCornerShape(12.dp)
                                                ), contentAlignment = Alignment.Center
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
                            })
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val seconds = state.timerSeconds.toString()

                        if (!state.canResend) {
                            Text(
                                text = stringResource(R.string.otp_resend_timer, seconds),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.otp_resend_active),
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.yellow,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    onEvent(OtpEvent.ResendClicked(phoneNumber))
                                })
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    val isEnabled = state.otpCode.length == otpLength && !state.isVerifying
                    Button(
                        onClick = { onEvent(OtpEvent.VerifyClicked(phoneNumber)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                8.dp,
                                RoundedCornerShape(12.dp),
                                spotColor = colors.yellow.copy(alpha = 0.3f)
                            ),
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
                                    else Brush.linearGradient(
                                        listOf(Color.Transparent, Color.Transparent)
                                    )
                                ), contentAlignment = Alignment.Center
                        ) {
                            if (state.isVerifying) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.otp_verify_code_button),
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 32.dp, start = 24.dp)
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clickable { onBackClick() }, contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = AppIcons.Back,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "1. OTP Light Mode")
@Composable
fun OtpScreenPreview() {
    ParentControlTheme {
        OtpScreenContent(
            state = OtpState(otpCode = "12", timerSeconds = 60, authError = null),
            phoneNumber = "09123456789",
            onEvent = {},
            onBackClick = {})
    }
}