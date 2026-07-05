package com.vahak.mehrban.uiv2.components.browser.safebrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.mehrban.R
import com.vahak.mehrban.presentation.browser.BrowserEvent
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun BrowserSearchBar(
    inputText: String,
    currentUrl: String,
    isLoading: Boolean,
    onEvent: (BrowserEvent) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit
) {
    val colors = LocalCustomColors.current
    val focusManager = LocalFocusManager.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(inputText)) }
    var isUrlFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isUrlFocused) {
        if (isUrlFocused && textFieldValue.text.isNotEmpty()) {
            delay(50.milliseconds)
            textFieldValue = textFieldValue.copy(selection = TextRange(0, textFieldValue.text.length))
        }
    }

    LaunchedEffect(inputText) {
        if (!isUrlFocused && inputText != textFieldValue.text) {
            textFieldValue = TextFieldValue(text = inputText)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = {
                    textFieldValue = it
                    onEvent(BrowserEvent.InputChanged(it.text))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .onFocusChanged { isUrlFocused = it.isFocused },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.browser_search_hint), color = colors.textHint) },
                trailingIcon = {
                    if (isUrlFocused && textFieldValue.text.isNotEmpty()) {
                        IconButton(onClick = {
                            textFieldValue = TextFieldValue("")
                            onEvent(BrowserEvent.InputChanged(""))
                        }) {
                            Icon(AppIcons.Close, contentDescription = "Clear", tint = colors.textHint, modifier = Modifier.size(20.dp))
                        }
                    } else if (isLoading) {
                        IconButton(onClick = onStop) {
                            Icon(AppIcons.Close, contentDescription = "Stop", tint = colors.red, modifier = Modifier.size(20.dp))
                        }
                    } else if (currentUrl.isNotEmpty()) {
                        IconButton(onClick = onReload) {
                            Icon(AppIcons.Refresh, contentDescription = "Reload", tint = colors.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                    unfocusedContainerColor = colors.cardInnerBG, focusedContainerColor = colors.surface,
                    focusedBorderColor = colors.primary, unfocusedBorderColor = colors.divider,
                    cursorColor = colors.primary
                ),
                textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    onEvent(BrowserEvent.SubmitSearch)
                    focusManager.clearFocus()
                })
            )
        }
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewBrowserSearchBar() {
    ParentControlTheme {
        BrowserSearchBar(
            inputText = "aparat.com",
            currentUrl = "https://aparat.com",
            isLoading = false,
            onEvent = {},
            onReload = {},
            onStop = {}
        )
    }
}