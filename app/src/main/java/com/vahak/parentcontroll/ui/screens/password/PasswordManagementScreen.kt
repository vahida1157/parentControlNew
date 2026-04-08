package com.vahak.parentcontroll.ui.screens.password

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.password.*
import com.vahak.parentcontroll.ui.component.PinInputComponent
import com.vahak.parentcontroll.ui.component.SimpleFlatHeader
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

// --- 1. STATEFUL WRAPPER (Used by NavGraph) ---
@Composable
fun PasswordManagementScreen(
    viewModel: PasswordManagementViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PasswordEffect.NavigateBack -> onBackClick()
                is PasswordEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    PasswordManagementContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick
    )
}

// --- 2. STATELESS CONTENT (Used for Previews) ---
@Composable
fun PasswordManagementContent(
    state: PasswordState,
    onEvent: (PasswordEvent) -> Unit,
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        SimpleFlatHeader(
            title = "رمز عبور والدین",
            onBackClick = { onEvent(PasswordEvent.BackClicked) }
        )

        if (state.step != PasswordStep.LOADING) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                val instructionText = when (state.step) {
                    PasswordStep.ENTER_CURRENT -> "لطفاً رمز عبور فعلی خود را وارد کنید"
                    PasswordStep.ENTER_NEW -> "رمز عبور ۴ رقمی جدید خود را وارد کنید"
                    PasswordStep.CONFIRM_NEW -> "رمز عبور جدید را دوباره وارد کنید"
                    else -> ""
                }

                Text(
                    text = instructionText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(30.dp))

                PinInputComponent(
                    pin = state.enteredPin,
                    onPinChange = { onEvent(PasswordEvent.PinChanged(it)) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = colors.red,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Setup New PIN", locale = "fa")
@Composable
fun PasswordScreenNewPinPreview() {
    ParentControlTheme {
        PasswordManagementContent(
            state = PasswordState(
                step = PasswordStep.ENTER_NEW,
                enteredPin = "",
                errorMessage = null
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Confirm PIN Error", locale = "fa")
@Composable
fun PasswordScreenConfirmErrorPreview() {
    ParentControlTheme {
        PasswordManagementContent(
            state = PasswordState(
                step = PasswordStep.CONFIRM_NEW,
                enteredPin = "12", // Partially filled wrong pin
                errorMessage = "رمز عبور مطابقت ندارد. دوباره تلاش کنید."
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}