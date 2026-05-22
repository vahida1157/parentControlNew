package com.vahak.mehrban.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme

@Composable
fun PinInputComponent(
    modifier: Modifier = Modifier, pinLength: Int = 5, pin: String, onPinChange: (String) -> Unit
) {
    val colors = LocalCustomColors.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Hidden TextField that handles the actual keyboard input
    BasicTextField(
        value = pin,
        onValueChange = {
            if (it.length <= pinLength && it.all { char -> char.isDigit() }) {
                onPinChange(it)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier
            .size(1.dp) // Hide it from view!
            .focusRequester(focusRequester)
    )

    // The Visual PIN Boxes
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                focusRequester.requestFocus()
                keyboardController?.show()
            }, horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        repeat(pinLength) { index ->
            val char = when {
                index >= pin.length -> ""
                else -> "●" // Show a dot for security
            }

            val isFocused = index == pin.length // Highlight the next box to be typed

            Box(
                modifier = Modifier
                    .size((250 / pinLength).dp)
                    .background(colors.surface, RoundedCornerShape(15.dp))
                    .border(
                        width = if (isFocused) 2.dp else 1.dp,
                        color = if (isFocused) colors.primary else colors.divider,
                        shape = RoundedCornerShape(15.dp)
                    ), contentAlignment = Alignment.Center
            ) {
                Text(
                    text = char,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "1. Empty PIN", locale = "fa")
@Composable
fun PinInputEmptyPreview() {
    ParentControlTheme {
        Box(
            modifier = Modifier
                .padding(20.dp)
                .background(LocalCustomColors.current.background)
        ) {
            PinInputComponent(pin = "", onPinChange = {})
        }
    }
}

@Preview(showBackground = true, name = "2. Partially Filled PIN", locale = "fa")
@Composable
fun PinInputPartialPreview() {
    ParentControlTheme {
        Box(
            modifier = Modifier
                .padding(20.dp)
                .background(LocalCustomColors.current.background)
        ) {
            // Shows what it looks like when 2 numbers are typed
            PinInputComponent(pin = "12", onPinChange = {})
        }
    }
}