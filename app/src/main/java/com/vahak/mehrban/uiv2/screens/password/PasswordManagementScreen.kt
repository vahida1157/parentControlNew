package com.vahak.mehrban.uiv2.screens.password

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.presentation.password.PasswordEffectV2
import com.vahak.mehrban.presentation.password.PasswordEventV2
import com.vahak.mehrban.presentation.password.PasswordStateV2
import com.vahak.mehrban.presentation.password.PasswordStrength
import com.vahak.mehrban.presentation.password.PasswordViewModelV2
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun PasswordManagementScreen(
    viewModel: PasswordViewModelV2 = hiltViewModel(), onNavigateToDashboard: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PasswordEffectV2.NavigateToDashboard -> onNavigateToDashboard()
                is PasswordEffectV2.ShowToast -> Toast.makeText(
                    context, effect.message, Toast.LENGTH_SHORT
                ).show()

                PasswordEffectV2.NavigateBack -> {}
            }
        }
    }

    PasswordManagementScreenContent(
        state = state, questionsList = viewModel.questionsList, onEvent = viewModel::onEvent
    )
}

@Composable
fun PasswordManagementScreenContent(
    state: PasswordStateV2, questionsList: List<String>, onEvent: (PasswordEventV2) -> Unit
) {
    val colors = LocalCustomColors.current
    val isDark = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(colors.primary, colors.primaryVariant, Color(0xFF0A4F46))
    )

    val goldGradient = Brush.linearGradient(
        colors = listOf(colors.yellow, colors.orange)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Gold Shield/Key Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(goldGradient, CircleShape)
                    .shadow(16.dp, CircleShape, spotColor = colors.yellow.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = AppIcons.LockBadge,
                    contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.password_setup_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.password_setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Main Form Card
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // --- 1. SECURITY QUESTION ---
                    var expanded by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.password_security_question_label),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.cardInnerBG, RoundedCornerShape(12.dp))
                                .border(2.dp, colors.divider, RoundedCornerShape(12.dp))
                                .clickable { expanded = true }
                                .padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.selectedQuestion,
                                    color = colors.textPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Icon(
                                    painter = AppIcons.ChevronDown,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(colors.surface)
                            ) {
                                questionsList.forEach { q ->
                                    DropdownMenuItem(
                                        text = { Text(q, color = colors.textPrimary) },
                                        onClick = {
                                            onEvent(PasswordEventV2.QuestionSelected(q))
                                            expanded = false
                                        })
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.securityAnswer,
                        onValueChange = { onEvent(PasswordEventV2.AnswerChanged(it.trim())) },
                        placeholder = { Text(stringResource(R.string.password_answer_placeholder), color = colors.textHint) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            unfocusedContainerColor = colors.cardInnerBG,
                            focusedContainerColor = colors.surface,
                            unfocusedBorderColor = colors.divider,
                            focusedBorderColor = colors.primary,
                        )
                    )

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.divider)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // --- 2. PASSWORD INPUT ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.password_label),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = state.passwordInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { char -> char.isDigit() }
                                    if (filtered.length <= 8) onEvent(
                                        PasswordEventV2.PasswordChanged(filtered)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = if (state.isPasswordVisible) 4.sp else 8.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { onEvent(PasswordEventV2.TogglePasswordVisibility) }) {
                                        Icon(
                                            painter = if (state.isPasswordVisible) AppIcons.VisibilityOff else AppIcons.Visibility,
                                            contentDescription = null,
                                            tint = if (state.isPasswordVisible) colors.primary else colors.textHint
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = colors.cardInnerBG,
                                    focusedContainerColor = colors.surface,
                                    unfocusedBorderColor = colors.divider,
                                    focusedBorderColor = colors.primary,
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Password Strength Bar
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val barColor by animateColorAsState(
                                targetValue = when (state.passwordStrength) {
                                    PasswordStrength.NONE -> colors.divider
                                    PasswordStrength.WEAK -> colors.red
                                    PasswordStrength.MEDIUM -> colors.yellow
                                    PasswordStrength.STRONG -> colors.primary
                                }
                            )
                            val barWeight by animateFloatAsState(
                                targetValue = when (state.passwordStrength) {
                                    PasswordStrength.NONE -> 0f
                                    PasswordStrength.WEAK -> 0.33f
                                    PasswordStrength.MEDIUM -> 0.66f
                                    PasswordStrength.STRONG -> 1f
                                }
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .background(colors.divider, RoundedCornerShape(3.dp))
                            ) {
                                if (barWeight > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(barWeight)
                                            .height(5.dp)
                                            .background(barColor, RoundedCornerShape(3.dp))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            val statusText = when (state.passwordStrength) {
                                PasswordStrength.NONE -> stringResource(R.string.password_strength_none)
                                PasswordStrength.WEAK -> stringResource(R.string.password_strength_weak)
                                PasswordStrength.MEDIUM -> stringResource(R.string.password_strength_medium)
                                PasswordStrength.STRONG -> stringResource(R.string.password_strength_strong)
                            }
                            Text(
                                text = statusText,
                                color = if (state.passwordStrength == PasswordStrength.NONE) colors.textHint else barColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // --- 3. CONFIRM PASSWORD ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.password_confirm_label),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = state.confirmPasswordInput,
                                onValueChange = {
                                    val filtered = it.filter { char -> char.isDigit() }
                                    if (filtered.length <= 8) onEvent(
                                        PasswordEventV2.ConfirmPasswordChanged(filtered)
                                    )
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (state.isFormValid && !state.isLoading) {
                                            onEvent(PasswordEventV2.SubmitClicked)
                                        }
                                    }
                                ),
                                visualTransformation = if (state.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = if (state.isConfirmPasswordVisible) 4.sp else 8.sp,
                                    fontWeight = FontWeight.Black
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { onEvent(PasswordEventV2.ToggleConfirmPasswordVisibility) }) {
                                        Icon(
                                            painter = if (state.isConfirmPasswordVisible) AppIcons.VisibilityOff else AppIcons.Visibility,
                                            contentDescription = null,
                                            tint = if (state.isConfirmPasswordVisible) colors.primary else colors.textHint
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = colors.cardInnerBG,
                                    focusedContainerColor = colors.surface,
                                    unfocusedBorderColor = colors.divider,
                                    focusedBorderColor = colors.primary,
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Match Message
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.confirmPasswordInput.isNotEmpty()) {
                            if (state.passwordsMatch) {
                                Text(
                                    stringResource(R.string.password_match_success),
                                    color = colors.primary,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            } else {
                                Text(
                                    stringResource(R.string.password_match_error),
                                    color = colors.red,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage,
                            color = colors.red,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- 4. SUBMIT BUTTON ---
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onEvent(PasswordEventV2.SubmitClicked)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                8.dp,
                                RoundedCornerShape(12.dp),
                                spotColor = colors.primary.copy(alpha = 0.3f)
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryVariant,
                            disabledContainerColor = colors.divider
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = state.isFormValid && !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = colors.textOnPrimaryVariant,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.password_submit_button),
                                color = colors.textOnPrimaryVariant,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.password_footer_note),
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================
@Preview(showBackground = true, locale = "fa", name = "1. Password Setup V2 (Light)")
@Composable
fun PreviewPasswordManagementLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        PasswordManagementScreenContent(
            state = PasswordStateV2(), questionsList = listOf(
                "نام اولین معلم شما چیست؟",
                "نام حیوان خانگی مورد علاقه شما در کودکی؟",
                "نام شهر محل تولد مادرتان چیست؟"
            ), onEvent = {})
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Password Setup V2 (Dark, Filled)")
@Composable
fun PreviewPasswordManagementDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        PasswordManagementScreenContent(
            state = PasswordStateV2(), questionsList = listOf(
                "نام حیوان خانگی مورد علاقه شما در کودکی؟",
                "نام اولین معلم شما چیست؟",
                "نام شهر محل تولد مادرتان چیست؟"
            ), onEvent = {})
    }
}