package com.vahak.mehrban.ui.screens.password

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.presentation.password.PasswordEffect
import com.vahak.mehrban.presentation.password.PasswordEvent
import com.vahak.mehrban.presentation.password.PasswordManagementViewModel
import com.vahak.mehrban.presentation.password.PasswordState
import com.vahak.mehrban.presentation.password.PasswordStep
import com.vahak.mehrban.ui.component.AnimatedPinDots
import com.vahak.mehrban.ui.component.CustomNumpad
import com.vahak.mehrban.ui.component.SimpleFlatHeader
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme

// --- 1. STATEFUL WRAPPER ---
@Composable
fun PasswordManagementScreen(
    viewModel: PasswordManagementViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToDashboard: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PasswordEffect.NavigateBack -> onBackClick()
                is PasswordEffect.NavigateToDashboard -> onNavigateToDashboard()
                is PasswordEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    PasswordManagementContent(
        state = state,
        questionsList = viewModel.questionsList,
        onEvent = viewModel::onEvent
    )
}

// --- 2. STATELESS CONTENT ---
@Composable
fun PasswordManagementContent(
    state: PasswordState,
    questionsList: List<String>,
    onEvent: (PasswordEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        val title = when (state.step) {
            PasswordStep.SETUP_QUESTION, PasswordStep.SETUP_PIN -> "تنظیمات اولیه"
            PasswordStep.RECOVER -> "بازیابی"
            else -> "تنظیمات والدین"
        }

        SimpleFlatHeader(title = title, onBackClick = { onEvent(PasswordEvent.BackClicked) })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state.step) {
                PasswordStep.LOADING -> CircularProgressIndicator(color = colors.primary)

                PasswordStep.SETUP_QUESTION -> SetupQuestionView(state, questionsList, onEvent)

                PasswordStep.RECOVER -> RecoverView(state, onEvent)

                PasswordStep.SETUP_PIN, PasswordStep.ENTER_CURRENT, PasswordStep.ENTER_NEW -> {
                    PinInputView(state, onEvent)
                }
            }
        }
    }
}

@Composable
fun SetupQuestionView(
    state: PasswordState,
    questionsList: List<String>,
    onEvent: (PasswordEvent) -> Unit
) {
    val colors = LocalCustomColors.current
    var expanded by remember { mutableStateOf(false) }

    Text(
        "سوال بازیابی رمز عبور",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
    )
    Text(
        text = "لطفاً یک سوال امنیتی انتخاب کنید. در صورت فراموشی رمز، به این پاسخ نیاز خواهید داشت.",
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 15.dp)
    )

    // Dropdown for Question
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Text("انتخاب سوال:", color = colors.textPrimary, modifier = Modifier.padding(bottom = 8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(15.dp)
        ) {
            Text(state.selectedQuestion, color = colors.textPrimary)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                questionsList.forEach { q ->
                    DropdownMenuItem(text = { Text(q) }, onClick = {
                        onEvent(PasswordEvent.QuestionSelected(q))
                        expanded = false
                    })
                }
            }
        }
    }

    // Answer Input
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    ) {
        Text("پاسخ شما:", color = colors.textPrimary, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = state.securityAnswer.trim(),
            onValueChange = { onEvent(PasswordEvent.AnswerChanged(it.trim())) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.divider,
                cursorColor = colors.primary,
            )
        )
    }

    if (state.errorMessage != null) {
        Text(state.errorMessage, color = colors.red, modifier = Modifier.padding(bottom = 15.dp))
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = { onEvent(PasswordEvent.SubmitSetupQuestion) },
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("تایید و مرحله بعد", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RecoverView(state: PasswordState, onEvent: (PasswordEvent) -> Unit) {
    val colors = LocalCustomColors.current

    Text(
        "بازیابی رمز عبور",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary
    )
    Text(
        text = "به سوال امنیتی زیر پاسخ دهید تا بتوانید رمز جدیدی تنظیم کنید.",
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 15.dp)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(15.dp)
    ) {
        Text("سوال امنیتی شما:", color = colors.primary, fontWeight = FontWeight.Bold)
        Text(
            state.savedQuestion ?: "",
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 5.dp)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedTextField(
        value = state.securityAnswer,
        onValueChange = { onEvent(PasswordEvent.AnswerChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text("پاسخ خود را وارد کنید...") }
    )

    if (state.errorMessage != null) {
        Text(state.errorMessage, color = colors.red, modifier = Modifier.padding(top = 10.dp))
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = { onEvent(PasswordEvent.SubmitRecoveryAnswer) },
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text("بررسی پاسخ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PinInputView(state: PasswordState, onEvent: (PasswordEvent) -> Unit) {
    val colors = LocalCustomColors.current

    val title = when (state.step) {
        PasswordStep.SETUP_PIN -> "تنظیم رمز ۵ رقمی"
        PasswordStep.ENTER_CURRENT -> "تغییر رمز عبور"
        PasswordStep.ENTER_NEW -> "رمز عبور جدید"
        else -> ""
    }

    val desc = when (state.step) {
        PasswordStep.SETUP_PIN -> "یک رمز عبور برای خروج از حالت فرزند و تنظیمات تعیین کنید."
        PasswordStep.ENTER_CURRENT -> "ابتدا رمز عبور فعلی خود را وارد کنید."
        PasswordStep.ENTER_NEW -> "حالا رمز ۵ رقمی جدید خود را وارد کنید."
        else -> ""
    }

    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
    Text(
        text = desc,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 15.dp)
    )

    AnimatedPinDots(
        currentInputLength = state.enteredPin.length,
        isError = state.isError
    )

    if (state.errorMessage != null) {
        Text(state.errorMessage, color = colors.red, fontSize = 14.sp)
    }

    if (state.step == PasswordStep.ENTER_CURRENT) {
        TextButton(
            onClick = { onEvent(PasswordEvent.ForgotPinClicked) },
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text(
                "رمز عبور را فراموش کرده‌اید؟",
                color = colors.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    CustomNumpad(
        onNumberClick = { onEvent(PasswordEvent.NumberClicked(it)) },
        onBackspaceClick = { onEvent(PasswordEvent.BackspaceClicked) },
        onClearClick = { onEvent(PasswordEvent.ClearClicked) }
    )
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, locale = "fa", name = "1. Setup Question")
@Composable
fun PreviewSetupQuestion() {
    ParentControlTheme {
        PasswordManagementContent(
            state = PasswordState(step = PasswordStep.SETUP_QUESTION),
            questionsList = listOf("نام حیوان خانگی شما؟"),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Setup PIN")
@Composable
fun PreviewSetupPin() {
    ParentControlTheme {
        PasswordManagementContent(
            state = PasswordState(step = PasswordStep.SETUP_PIN, enteredPin = "123"),
            questionsList = emptyList(),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "3. Recover PIN")
@Composable
fun PreviewRecover() {
    ParentControlTheme {
        PasswordManagementContent(
            state = PasswordState(
                step = PasswordStep.RECOVER,
                savedQuestion = "نام اولین معلم شما چیست؟"
            ),
            questionsList = emptyList(),
            onEvent = {}
        )
    }
}