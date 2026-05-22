package com.vahak.mehrban.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme
import kotlinx.coroutines.delay

@Composable
fun PinEntryDialog(
    title: String = "ورود رمز عبور",
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var currentPin by remember { mutableStateOf("") }
    val pinLength = 5

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        // 🚀 PRO FIX 1: Extracted the content so the Preview tool can see it!
        PinEntryDialogContent(
            title = title,
            errorMessage = errorMessage,
            currentPin = currentPin,
            pinLength = pinLength,
            onPinChange = { newPin ->
                if (newPin.length <= pinLength && newPin.all { it.isDigit() }) {
                    currentPin = newPin
                    if (newPin.length == pinLength) {
                        onSubmit(newPin)
                        currentPin = ""
                    }
                }
            },
            onDismiss = onDismiss
        )
    }
}

// 🚀 STATELESS CONTENT FOR PREVIEWS
@Composable
fun PinEntryDialogContent(
    title: String,
    errorMessage: String?,
    currentPin: String,
    pinLength: Int,
    onPinChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            PinInputComponent(
                pin = currentPin,
                pinLength = pinLength,
                onPinChange = onPinChange
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = colors.red,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(onClick = onDismiss) {
            Text("انصراف", color = colors.textSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PinInputComponent(
    pin: String,
    pinLength: Int,
    onPinChange: (String) -> Unit
) {
    val colors = LocalCustomColors.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    BasicTextField(
        value = pin,
        onValueChange = onPinChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.focusRequester(focusRequester),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pinLength) { index ->
                    // 🚀 PRO FIX 2: Show the bullet instead of the number!
                    val char = when {
                        index >= pin.length -> ""
                        else -> "•" // Password mask
                    }

                    val isFocusedBox = index == pin.length
                    val borderColor = if (isFocusedBox) colors.primary else colors.divider
                    val boxBgColor = if (char.isNotEmpty()) colors.primary.copy(alpha = 0.1f) else Color.Transparent

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(boxBgColor)
                            .border(
                                width = if (isFocusedBox) 2.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

// ============================================================================
// PREVIEWS
// ============================================================================

@Preview(showBackground = true, locale = "fa", name = "1. PIN Dialog (Normal)")
@Composable
fun PinEntryDialogPreview() {
    ParentControlTheme {
        Box(modifier = Modifier.padding(20.dp).background(Color.Gray)) {
            PinEntryDialogContent(
                title = "ورود رمز عبور",
                errorMessage = null,
                currentPin = "123", // Shows 3 bullets
                pinLength = 5,
                onPinChange = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. PIN Dialog (Error)")
@Composable
fun PinEntryDialogErrorPreview() {
    ParentControlTheme {
        Box(modifier = Modifier.padding(20.dp).background(Color.Gray)) {
            PinEntryDialogContent(
                title = "ورود رمز عبور",
                errorMessage = "رمز عبور اشتباه است",
                currentPin = "",
                pinLength = 5,
                onPinChange = {},
                onDismiss = {}
            )
        }
    }
}