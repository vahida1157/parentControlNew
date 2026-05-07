package com.vahak.parentcontroll.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.addchild.AddChildEffect
import com.vahak.parentcontroll.presentation.addchild.AddChildEvent
import com.vahak.parentcontroll.presentation.addchild.AddChildState
import com.vahak.parentcontroll.presentation.addchild.AddChildViewModel
import com.vahak.parentcontroll.ui.component.AvatarPickerBadge
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

enum class Gender {
    Girl, Boy
}

// 1. STATEFUL WRAPPER (Used by NavGraph)
@Composable
fun AddChildScreen(
    viewModel: AddChildViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Listen for One-Off Effects from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AddChildEffect.NavigateBack -> onBackClick()
                is AddChildEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Pass data down to the Stateless UI
    AddChildScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick
    )
}

// 2. STATELESS UI (Used for rendering and Previews)
@Composable
fun AddChildScreenContent(
    state: AddChildState,
    onEvent: (AddChildEvent) -> Unit,
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {

        // 1. Reusable Header
        _root_ide_package_.com.vahak.parentcontroll.ui.component.SimpleCurvedHeader(
            title = "افزودن فرزند جدید",
            onBackClick = onBackClick
        )

        // 2. Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {

                // Form Card
                Card(
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 55.dp, bottom = 30.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 70.dp, bottom = 30.dp, start = 25.dp, end = 25.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        // Error Message Display
                        if (state.errorMessage != null) {
                            Text(
                                text = state.errorMessage,
                                color = colors.red,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { onEvent(AddChildEvent.NameChanged(it)) },
                            placeholder = { Text("نام کودک (مثلا: علی)", color = colors.textHint) },
                            singleLine = true,
                            isError = state.errorMessage?.contains("نام") == true,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.divider,
                                cursorColor = colors.primary
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEvent(AddChildEvent.OpenDobSheet) }
                        ) {
                            OutlinedTextField(
                                value = state.dob,
                                onValueChange = { },
                                enabled = false,
                                readOnly = true,
                                placeholder = {
                                    Text("تاریخ تولد", color = colors.textHint)
                                },
                                // Keep the colors looking "active" even though it's technically disabled
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = colors.textPrimary,
                                    disabledBorderColor = if (state.errorMessage?.contains("تاریخ") == true) colors.red else colors.divider,
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            _root_ide_package_.com.vahak.parentcontroll.ui.component.GenderOption(
                                isSelected = state.gender == Gender.Girl,
                                title = "دختر",
                                icon = AppIcons.Female,
                                activeColor = colors.red,
                                modifier = Modifier.weight(1f)
                            ) { onEvent(AddChildEvent.GenderSelected(Gender.Girl)) }

                            _root_ide_package_.com.vahak.parentcontroll.ui.component.GenderOption(
                                isSelected = state.gender == Gender.Boy,
                                title = "پسر",
                                icon = AppIcons.Male,
                                activeColor = colors.blue,
                                modifier = Modifier.weight(1f)
                            ) { onEvent(AddChildEvent.GenderSelected(Gender.Boy)) }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = { onEvent(AddChildEvent.SaveClicked) },
                            enabled = !state.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.yellow,
                                contentColor = colors.textPrimary,
                                disabledContainerColor = colors.backgroundButtonDisable
                            )
                        ) {
                            Text(
                                text = if (state.isSaving) "در حال ذخیره..." else "ذخیره اطلاعات",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!state.isSaving) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(
                                    painter = AppIcons.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Reusable Profile Uploader
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    AvatarPickerBadge(
                        onClick = { /* Open Image Picker */ })
                }
            }

            if (state.isDobSheetOpen) {
                // Safely parse the current DOB string back into numbers
                val dobParts = state.dob.split("/")
                val currentYear = dobParts.getOrNull(0)?.toIntOrNull() ?: 1395
                val currentMonth = dobParts.getOrNull(1)?.toIntOrNull() ?: 1
                val currentDay = dobParts.getOrNull(2)?.toIntOrNull() ?: 1

                _root_ide_package_.com.vahak.parentcontroll.ui.component.DynamicDatePicker(
                    title = "تاریخ تولد فرزند",
                    mode = com.vahak.parentcontroll.ui.component.PickerPresentationMode.DIALOG,
                    yearRange = 1380..1403,
                    monthRange = 1..12,
                    dayRange = 1..31,
                    initialYear = currentYear,
                    initialMonth = currentMonth,
                    initialDay = currentDay,
                    onDismiss = { onEvent(AddChildEvent.CloseDobSheet) },
                    onConfirm = { y, m, d ->
                        onEvent(AddChildEvent.DobSelected(y, m, d))
                    }
                )
            }
        }
    }
}

// 3. SAFE PREVIEWS
@Preview(showBackground = true, name = "Add Child Light", locale = "fa")
@Composable
fun AddChildPreviewLight() {
    ParentControlTheme(darkTheme = false) {
        // Use the Stateless Content for Previews!
        AddChildScreenContent(
            state = AddChildState(),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Add Child Dark", locale = "fa")
@Composable
fun AddChildPreviewDark() {
    ParentControlTheme(darkTheme = true) {
        // Use the Stateless Content for Previews!
        AddChildScreenContent(
            state = AddChildState(),
            onEvent = {},
            onBackClick = {}
        )
    }
}