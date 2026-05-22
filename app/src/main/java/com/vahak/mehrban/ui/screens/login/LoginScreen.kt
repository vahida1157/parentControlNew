package com.vahak.mehrban.ui.screens.login

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.presentation.login.LoginEffect
import com.vahak.mehrban.presentation.login.LoginEvent
import com.vahak.mehrban.presentation.login.LoginState
import com.vahak.mehrban.presentation.login.LoginViewModel
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme

// 1. STATEFUL WRAPPER
// This handles the ViewModel, State Collection, and Navigation Effects.
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

    // Pass everything down to the Stateless component
    LoginScreenContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

// 2. STATELESS CONTENT (Safe for Previews)
// This only knows about UI logic. It takes data in (state) and sends events up (onEvent).
@Composable
fun LoginScreenContent(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    val primaryGradient = Brush.linearGradient(
        colors = listOf(colors.primary, colors.secondary)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    10.dp,
                    RoundedCornerShape(30.dp),
                    ambientColor = Color.Black.copy(alpha = 0.05f)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo Area
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(primaryGradient, RoundedCornerShape(20.dp))
                        .shadow(
                            8.dp,
                            RoundedCornerShape(20.dp),
                            spotColor = colors.primary.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = AppIcons.Phone,
                        contentDescription = "Mobile Icon",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "صفحه ورود",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Form Group
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage,
                            color = colors.red,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Text(
                        text = "شماره تماس پدر یا مادر",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = state.phoneNumber,
                        onValueChange = { onEvent(LoginEvent.PhoneChanged(it)) }, // Replaced viewModel call
                        placeholder = {
                            Text(
                                text = "لطفا شماره تماس خود را وارد کنید مانند : 09102112222",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textHint,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDirection = TextDirection.Ltr,
                            textAlign = TextAlign.Start,
                            color = colors.textPrimary
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8F9FA),
                            focusedContainerColor = colors.surface,
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            focusedBorderColor = colors.primary,
                            cursorColor = colors.primary,
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(25.dp))

                // Info Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(16.dp))
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = AppIcons.Info,
                        contentDescription = "Info",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "کد ورود برای شما پیامک می‌شود.",
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Submit Button
                Button(
                    onClick = { onEvent(LoginEvent.SubmitClicked) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(),
                    enabled = !state.isLoading // Disable button while network request is happening
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (!state.isLoading) primaryGradient
                                else Brush.linearGradient(listOf(Color.Gray, Color.LightGray))
                            )
                            .shadow(
                                10.dp,
                                RoundedCornerShape(18.dp),
                                spotColor = if (!state.isLoading) colors.primary.copy(alpha = 0.3f) else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // FIX: Added Loading Indicator
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "دریافت کد تایید",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    painter = AppIcons.ChevronLeft,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
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
        // We now preview the Stateless component and mock the data
        LoginScreenContent(
            state = LoginState(phoneNumber = ""),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Loading State")
@Composable
fun LoginScreenLoadingPreview() {
    ParentControlTheme {
        LoginScreenContent(
            state = LoginState(phoneNumber = "09123456789", isLoading = true),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "3. Error State")
@Composable
fun LoginScreenErrorPreview() {
    ParentControlTheme {
        LoginScreenContent(
            state = LoginState(
                phoneNumber = "123",
                errorMessage = "شماره تماس نامعتبر است (مثال: 09123456789)"
            ),
            onEvent = {}
        )
    }
}