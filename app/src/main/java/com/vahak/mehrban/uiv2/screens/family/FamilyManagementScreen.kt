package com.vahak.mehrban.uiv2.screens.family

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.core.data.local.entity.Gender
import com.vahak.mehrban.presentation.family.FamilyChildUi
import com.vahak.mehrban.presentation.family.FamilyEffect
import com.vahak.mehrban.presentation.family.FamilyEvent
import com.vahak.mehrban.presentation.family.FamilyState
import com.vahak.mehrban.presentation.family.FamilyViewModel
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import java.time.LocalDate

@Composable
fun FamilyManagementScreen(
    viewModel: FamilyViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onAddChildClick: () -> Unit,
    onChildSettingsClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FamilyEffect.NavigateBack -> onBackClick()
                is FamilyEffect.NavigateToAddChild -> onAddChildClick()
                is FamilyEffect.NavigateToChildSettings -> onChildSettingsClick(effect.childId)
            }
        }
    }

    FamilyManagementContent(
        state = state, onEvent = viewModel::onEvent
    )
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FamilyManagementContent(
    state: FamilyState, onEvent: (FamilyEvent) -> Unit
) {
    val colors = LocalCustomColors.current
    var childIdToDelete by remember { mutableStateOf<String?>(null) }

    if (childIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { childIdToDelete = null },
            containerColor = colors.surface,
            title = {
                Text(
                    stringResource(R.string.delete_child_title),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    stringResource(R.string.delete_child_message), color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEvent(FamilyEvent.DeleteChildClicked(childIdToDelete!!))
                        childIdToDelete = null
                    }) {
                    Text(
                        stringResource(R.string.delete),
                        color = colors.red,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { childIdToDelete = null }) {
                    Text(stringResource(R.string.cancel), color = colors.textPrimary)
                }
            })
    }
    Scaffold(
        containerColor = colors.background,
    ) { _ ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Header Section
            MehrbanHeader(
                title = stringResource(R.string.your_children),
                subtitle = stringResource(R.string.manage_children),
                action = HeaderAction.Add { onEvent(FamilyEvent.AddChildClicked) },
            )

            // 2. Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.tap_child_for_settings),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                } else if (state.children.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_children_added),
                        color = colors.textHint,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    state.children.forEach { childUi ->
                        ChildCardV2(
                            childUi = childUi,
                            onClick = { onEvent(FamilyEvent.ChildClicked(childUi.child.id)) },
                            onDeleteClick = { childIdToDelete = childUi.child.id })
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dashed Add Child Button
                AddChildDashedCardV2(
                    onClick = { onEvent(FamilyEvent.AddChildClicked) })

                Spacer(modifier = Modifier.height(100.dp)) // Buffer for bottom nav
            }
        }
    }
}

// ----------------------------------------------------------------------------
// EXTRACTED UI COMPONENTS
// ----------------------------------------------------------------------------

@Composable
fun ChildCardV2(
    childUi: FamilyChildUi, onClick: () -> Unit, onDeleteClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    val hours = childUi.usageSecondsToday / 3600
    val mins = (childUi.usageSecondsToday % 3600) / 60
    val ageText = if (childUi.ageYears > 0) {
        stringResource(R.string.age_years, childUi.ageYears)
    } else {
        stringResource(R.string.age_less_than_one)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.cardInnerBG, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (childUi.child.gender == Gender.BOY) "👦" else "👧", fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = childUi.child.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ageText, fontSize = 12.sp, color = colors.textSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%d:%02d", hours, mins),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.primary
                )
                Text(stringResource(R.string.today), fontSize = 10.sp, color = colors.textSecondary)
            }

            IconButton(
                onClick = { onDeleteClick() }, modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    painter = AppIcons.DeleteForever,
                    contentDescription = "Delete Child",
                    tint = colors.red.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun AddChildDashedCardV2(onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    val strokeColor = colors.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(colors.primary.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .drawBehind {
                val stroke = Stroke(
                    width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                )
                drawRoundRect(
                    color = strokeColor.copy(alpha = 0.5f),
                    style = stroke,
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(24.dp),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(colors.primary, colors.primaryVariant)),
                        CircleShape
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    AppIcons.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.add_new_child),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
    }
}

// ----------------------------------------------------------------------------
// PREVIEWS
// ----------------------------------------------------------------------------

private val mockChild1 =
    ChildEntity(id = "1", name = "علی", dob = LocalDate.now().minusYears(10), gender = Gender.BOY)
private val mockChild2 =
    ChildEntity(id = "2", name = "سارا", dob = LocalDate.now().minusYears(8), gender = Gender.GIRL)

private val mockChildUi1 =
    FamilyChildUi(child = mockChild1, ageYears = 10, usageSecondsToday = 6300)
private val mockChildUi2 = FamilyChildUi(child = mockChild2, ageYears = 8, usageSecondsToday = 3600)

@Preview(showBackground = true, name = "1. Family Management Light", locale = "fa")
@Composable
fun FamilyManagementPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        FamilyManagementContent(
            state = FamilyState(
                children = listOf(mockChildUi1, mockChildUi2), isLoading = false
            ), onEvent = {})
    }
}

@Preview(showBackground = true, name = "2. Family Management Dark", locale = "fa")
@Composable
fun FamilyManagementPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        FamilyManagementContent(
            state = FamilyState(
                children = listOf(mockChildUi1), isLoading = false
            ), onEvent = {})
    }
}

@Preview(showBackground = true, name = "3. Family Management Loading", locale = "fa")
@Composable
fun FamilyManagementPreviewLoading() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        FamilyManagementContent(
            state = FamilyState(
                children = emptyList(), isLoading = true
            ), onEvent = {})
    }
}