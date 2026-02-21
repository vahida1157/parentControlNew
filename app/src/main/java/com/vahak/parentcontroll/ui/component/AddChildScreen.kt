package com.vahak.parentcontroll.ui.component


import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vahak.parentcontroll.presentation.addchild.AddChildEffect
import com.vahak.parentcontroll.presentation.addchild.AddChildEvent
import com.vahak.parentcontroll.presentation.addchild.AddChildViewModel
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme


enum class Gender {
    Girl, Boy
}

@Composable
fun AddChildScreen(
    viewModel: AddChildViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {

        // 1. Reusable Header
        SimpleCurvedHeader(title = "افزودن فرزند جدید", onBackClick = onBackClick)

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
                                text = state.errorMessage!!,
                                color = colors.red,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { viewModel.onEvent(AddChildEvent.NameChanged(it)) },
                            placeholder = { Text("نام کودک (مثلا: علی)", color = colors.textHint) },
                            singleLine = true,
                            isError = state.errorMessage?.contains("نام") == true,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = state.dob,
                            onValueChange = { viewModel.onEvent(AddChildEvent.DobChanged(it)) },
                            placeholder = {
                                Text(
                                    "تاریخ تولد (مثلا: 1395/02/10)",
                                    color = colors.textHint
                                )
                            },
                            singleLine = true,
                            isError = state.errorMessage?.contains("تاریخ") == true,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(15.dp)
                        ) {
                            GenderOption(
                                isSelected = state.gender == Gender.Girl,
                                title = "دختر",
                                icon = AppIcons.Female,
                                activeColor = colors.red,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.onEvent(AddChildEvent.GenderSelected(Gender.Girl)) }

                            GenderOption(
                                isSelected = state.gender == Gender.Boy,
                                title = "پسر",
                                icon = AppIcons.Male,
                                activeColor = colors.blue,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.onEvent(AddChildEvent.GenderSelected(Gender.Boy)) }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        Button(
                            onClick = { viewModel.onEvent(AddChildEvent.SaveClicked) },
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
                    AvatarPickerBadge(onClick = { /* Open Image Picker */ })
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Add Child Light", locale = "fa")
@Composable
fun AddChildPreviewLight() {
    ParentControlTheme(darkTheme = false) {
        AddChildScreen(onBackClick = {})
    }
}

@Preview(showBackground = true, name = "Add Child Dark", locale = "fa")
@Composable
fun AddChildPreviewDark() {
    ParentControlTheme(darkTheme = true) {
        AddChildScreen(onBackClick = {})
    }
}