package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun PinEntryDialog(
    title: String = "ورود رمز عبور",
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val colors = LocalCustomColors.current
    var currentPin by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        // Prevent accidental dismissal if you want it super strict:
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
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

            PinInputComponent(
                pin = currentPin,
                onPinChange = { newPin ->
                    currentPin = newPin
                    // Auto-submit when 4 digits are entered!
                    if (newPin.length == 5) {
                        onSubmit(newPin)
                        currentPin = "" // Clear for next time
                    }
                }
            )

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
}

@Preview(showBackground = true, locale = "fa", name = "1. PIN Dialog (Normal)")
@Composable
fun PinEntryDialogPreview() {
    ParentControlTheme {
        PinEntryDialog(
            onDismiss = {},
            onSubmit = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. PIN Dialog (Error)")
@Composable
fun PinEntryDialogErrorPreview() {
    ParentControlTheme {
        PinEntryDialog(
            errorMessage = "رمز عبور اشتباه است",
            onDismiss = {},
            onSubmit = {}
        )
    }
}