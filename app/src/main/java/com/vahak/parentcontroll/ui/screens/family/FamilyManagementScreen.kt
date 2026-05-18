package com.vahak.parentcontroll.ui.screens.family

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.data.local.entity.Gender
import com.vahak.parentcontroll.presentation.family.FamilyChildUi
import com.vahak.parentcontroll.presentation.family.FamilyEffect
import com.vahak.parentcontroll.presentation.family.FamilyEvent
import com.vahak.parentcontroll.presentation.family.FamilyState
import com.vahak.parentcontroll.presentation.family.FamilyViewModel
import com.vahak.parentcontroll.ui.component.ClickableListItem
import com.vahak.parentcontroll.ui.component.ListSectionCard
import com.vahak.parentcontroll.ui.component.SimpleFlatHeader
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme
import java.time.LocalDate

// 1. STATEFUL WRAPPER
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
        state = state,
        onEvent = viewModel::onEvent
    )
}

// 2. STATELESS CONTENT
@Composable
fun FamilyManagementContent(
    state: FamilyState,
    onEvent: (FamilyEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    Scaffold(
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(FamilyEvent.AddChildClicked) },
                containerColor = colors.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 90.dp)
                    .size(60.dp) // Lifted above BottomNav area
            ) {
                Icon(
                    painter = AppIcons.Add,
                    contentDescription = "Add Child",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Start // RTL aligns to Left
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            SimpleFlatHeader(title = "خانواده", onBackClick = { onEvent(FamilyEvent.BackClicked) })

            // Scrollable Lists
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                // Section 1: Parents
                ListSectionCard(title = "سرپرست", headerIcon = AppIcons.Profile) {
                    ClickableListItem(
                        name = "پدر",
                        avatarIcon = AppIcons.Profile, // Swap with user-tie if you have it
                        avatarTint = Color.White,
                        avatarBg = colors.primaryVariant, // #80cbc4 equivalent
                        showDivider = false
                    )
                }

                // Section 2: Children
                ListSectionCard(
                    title = "فرزندان",
                    headerIcon = AppIcons.YoungChild
                ) {
                    if (state.children.isEmpty() && !state.isLoading) {
                        Text(
                            text = "هنوز فرزندی اضافه نشده است.",
                            color = colors.textHint,
                            modifier = Modifier.padding(20.dp)
                        )
                    } else {
                        state.children.forEachIndexed { index, child ->
                            // Alternate colors for avatars (Blue, Yellow, Green)
                            val (bg, tint) = when (index % 3) {
                                0 -> colors.blue.copy(alpha = 0.2f) to colors.blue
                                1 -> colors.yellow.copy(alpha = 0.3f) to colors.yellow // Using yellow instead of orange
                                else -> colors.primary.copy(alpha = 0.2f) to colors.primary
                            }

                            ClickableListItem(
                                name = child.child.name,
                                avatarIcon = AppIcons.YoungChild,
                                avatarBg = bg,
                                avatarTint = tint,
                                showDivider = index != state.children.lastIndex,
                                onClick = { onEvent(FamilyEvent.ChildClicked(child.child.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. PREVIEWS

private val mockChild1 =
    ChildEntity(id = "1", name = "علی", dob = LocalDate.now().minusYears(10), gender = Gender.BOY)
private val mockChild2 =
    ChildEntity(id = "2", name = "سارا", dob = LocalDate.now().minusYears(8), gender = Gender.GIRL)

// FIXED: Wrapped the mock ChildEntities into the required FamilyChildUi class
private val mockChildUi1 =
    FamilyChildUi(child = mockChild1, ageYears = 10, usageSecondsToday = 6300)
private val mockChildUi2 = FamilyChildUi(child = mockChild2, ageYears = 8, usageSecondsToday = 3600)

@Preview(showBackground = true, locale = "fa")
@Composable
fun FamilyManagementPreview() {
    ParentControlTheme {
        FamilyManagementContent(
            state = FamilyState(
                children = listOf(mockChildUi1, mockChildUi2),
                isLoading = false
            ),
            onEvent = {}
        )
    }
}