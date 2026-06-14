package com.vahak.mehrban.uiv2.screens.addchild

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.presentation.addchild.AddChildEffect
import com.vahak.mehrban.presentation.addchild.AddChildEvent
import com.vahak.mehrban.presentation.addchild.AddChildState
import com.vahak.mehrban.presentation.addchild.AddChildViewModel
import com.vahak.mehrban.ui.component.PickerPresentationMode
import com.vahak.mehrban.ui.screens.Gender
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.uiv2.components.DynamicDatePickerV2
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun AddChildScreen(
    viewModel: AddChildViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

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

    AddChildScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick
    )
}

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
            .systemBarsPadding()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeaderV2(
                title = stringResource(R.string.add_child_title),
                subtitle = stringResource(R.string.add_child_subtitle),
                onBackClick = onBackClick
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-30).dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.avatar_selection),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    var selectedCategory by remember { mutableStateOf("boys") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AvatarTabItem(stringResource(R.string.tab_boys), selectedCategory == "boys") {
                            selectedCategory = "boys"
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        AvatarTabItem(stringResource(R.string.tab_girls), selectedCategory == "girls") {
                            selectedCategory = "girls"
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        AvatarTabItem(stringResource(R.string.tab_teens), selectedCategory == "teens") {
                            selectedCategory = "teens"
                        }
                    }

                    val avatars = when (selectedCategory) {
                        "boys" -> listOf(1 to "👦", 2 to "👦🏻", 3 to "👦🏼", 4 to "👦🏽")
                        "girls" -> listOf(5 to "👧", 6 to "👧🏻", 7 to "👧🏼", 8 to "👧🏽")
                        else -> listOf(9 to "👨", 10 to "👩", 11 to "🧑", 12 to "👱")
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
                    ) {
                        items(avatars) { (id, emoji) ->
                            val isSelected = state.avatarId == id
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(colors.cardInnerBG, RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) colors.primary else Color.Transparent,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { onEvent(AddChildEvent.AvatarSelected(id)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 30.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.avatar_tip),
                        fontSize = 11.sp,
                        color = colors.textHint,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Intentionally disabled photo upload button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                colors.primary.copy(alpha = 0.05f),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = colors.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.upload_photo_button),
                            color = colors.primary,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = colors.divider.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage,
                            color = colors.red,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    FormInputGroup(label = stringResource(R.string.field_name)) {
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { onEvent(AddChildEvent.NameChanged(it)) },
                            placeholder = {
                                Text(
                                    stringResource(R.string.hint_child_name),
                                    color = colors.textHint
                                )
                            },
                            singleLine = true,
                            isError = state.errorMessage?.contains("نام") == true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                unfocusedContainerColor = colors.cardInnerBG,
                                focusedContainerColor = colors.surface,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.divider,
                                cursorColor = colors.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FormInputGroup(label = stringResource(R.string.field_dob)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEvent(AddChildEvent.OpenDobSheet) }) {
                            OutlinedTextField(
                                value = state.dob,
                                onValueChange = { },
                                enabled = false,
                                readOnly = true,
                                placeholder = {
                                    Text(
                                        stringResource(R.string.hint_dob),
                                        color = colors.textHint
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = colors.textPrimary,
                                    disabledContainerColor = colors.cardInnerBG,
                                    disabledBorderColor = if (state.errorMessage?.contains("تاریخ") == true) colors.red else colors.divider,
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FormInputGroup(label = stringResource(R.string.field_phone_optional)) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            OutlinedTextField(
                                value = state.phone,
                                onValueChange = {
                                    val filtered = it.filter { char -> char.isDigit() }
                                    if (filtered.length <= 11) onEvent(
                                        AddChildEvent.PhoneChanged(filtered)
                                    )
                                },
                                placeholder = { Text(stringResource(R.string.hint_phone), color = colors.textHint) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textAlign = TextAlign.Start,
                                    color = colors.textPrimary
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    unfocusedContainerColor = colors.cardInnerBG,
                                    focusedContainerColor = colors.surface,
                                    focusedBorderColor = colors.primary,
                                    unfocusedBorderColor = colors.divider,
                                    cursorColor = colors.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GenderChipV2(
                            isSelected = state.gender == Gender.Boy,
                            title = stringResource(R.string.gender_boy),
                            emoji = "👦",
                            activeColor = colors.blue,
                            modifier = Modifier.weight(1f)
                        ) { onEvent(AddChildEvent.GenderSelected(Gender.Boy)) }

                        GenderChipV2(
                            isSelected = state.gender == Gender.Girl,
                            title = stringResource(R.string.gender_girl),
                            emoji = "👧",
                            activeColor = colors.red,
                            modifier = Modifier.weight(1f)
                        ) { onEvent(AddChildEvent.GenderSelected(Gender.Girl)) }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onEvent(AddChildEvent.SaveClicked) },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                8.dp,
                                RoundedCornerShape(14.dp),
                                spotColor = colors.primary.copy(alpha = 0.4f)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            disabledContainerColor = colors.backgroundButtonDisable
                        )
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                color = colors.textOnPrimaryVariant,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                stringResource(R.string.button_add_child),
                                color = colors.textOnPrimaryVariant,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }

        if (state.isDobSheetOpen) {
            val dobParts = state.dob.split("/")
            val currentYear = dobParts.getOrNull(0)?.toIntOrNull() ?: 1395
            val currentMonth = dobParts.getOrNull(1)?.toIntOrNull() ?: 1
            val currentDay = dobParts.getOrNull(2)?.toIntOrNull() ?: 1

            DynamicDatePickerV2(
                title = stringResource(R.string.dob_picker_title),
                mode = PickerPresentationMode.BOTTOM_SHEET,
                yearRange = 1380..1405,
                monthRange = 1..12,
                dayRange = 1..31,
                initialYear = currentYear,
                initialMonth = currentMonth,
                initialDay = currentDay,
                onDismiss = { onEvent(AddChildEvent.CloseDobSheet) },
                onConfirm = { y, m, d -> onEvent(AddChildEvent.DobSelected(y, m, d)) }
            )
        }
    }
}

// --- SUB COMPONENTS ---

@Composable
fun ScreenHeaderV2(title: String, subtitle: String? = null, onBackClick: () -> Unit) {
    val colors = LocalCustomColors.current
    val headerGradient = Brush.linearGradient(listOf(colors.primary, colors.primaryVariant))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = headerGradient,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(top = 40.dp, bottom = 60.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if (subtitle != null) {
                    Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.Back, contentDescription = "Back", tint = Color.White)
            }
        }
    }
}

@Composable
fun AvatarTabItem(title: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) colors.surface else colors.cardInnerBG)
            .border(
                1.5.dp,
                if (isSelected) colors.primary else Color.Transparent,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = if (isSelected) colors.primary else colors.textSecondary,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun FormInputGroup(label: String, content: @Composable () -> Unit) {
    val colors = LocalCustomColors.current
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
fun GenderChipV2(
    isSelected: Boolean,
    title: String,
    emoji: String,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.1f) else colors.cardInnerBG)
            .border(
                2.dp,
                if (isSelected) activeColor else colors.divider,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = if (isSelected) activeColor else colors.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================
@Preview(showBackground = true, name = "1. Add Child Light", locale = "fa")
@Composable
fun AddChildPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        AddChildScreenContent(state = AddChildState(), onEvent = {}, onBackClick = {})
    }
}

@Preview(showBackground = true, name = "2. Add Child Dark", locale = "fa")
@Composable
fun AddChildPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        AddChildScreenContent(state = AddChildState(), onEvent = {}, onBackClick = {})
    }
}