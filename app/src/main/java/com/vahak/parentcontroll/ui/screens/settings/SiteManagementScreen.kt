package com.vahak.parentcontroll.ui.screens.settings

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.core.data.local.entity.BlockedDomainEntity
import com.vahak.parentcontroll.presentation.sitemanagement.SiteManagementEffect
import com.vahak.parentcontroll.presentation.sitemanagement.SiteManagementEvent
import com.vahak.parentcontroll.presentation.sitemanagement.SiteManagementState
import com.vahak.parentcontroll.presentation.sitemanagement.SiteManagementViewModel
import com.vahak.parentcontroll.ui.component.FeatureToggleCard
import com.vahak.parentcontroll.ui.component.SimpleFlatHeader
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun SiteManagementScreen(
    viewModel: SiteManagementViewModel = hiltViewModel(), onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SiteManagementEffect.NavigateBack -> onBackClick()
                is SiteManagementEffect.ShowToast -> Toast.makeText(
                    context, effect.message, Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    SiteManagementContent(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteManagementContent(
    state: SiteManagementState, onEvent: (SiteManagementEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SimpleFlatHeader(
                title = "مدیریت سایت‌ها",
                onBackClick = { onEvent(SiteManagementEvent.BackClicked) })

            Column(modifier = Modifier.padding(20.dp)) {
                // The Master VPN Toggle
                FeatureToggleCard(
                    title = "فیلتر هوشمند وب",
                    description = "با فعال کردن این بخش، دسترسی به سایت‌های مخرب و لیست سیاه مسدود می‌شود.",
                    isActive = state.isSiteManagementActive,
                    onToggle = { onEvent(SiteManagementEvent.ToggleActive(it)) })

                Spacer(modifier = Modifier.height(20.dp))

                // The Input Field to Add New Domains
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (state.isSiteManagementActive) 1f else 0.5f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.domainInput,
                        onValueChange = { onEvent(SiteManagementEvent.DomainInputChanged(it)) },
                        placeholder = { Text("مثال: badwebsite.com", color = colors.textHint) },
                        modifier = Modifier.weight(1f),
                        enabled = state.isSiteManagementActive,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.divider,
                            cursorColor = colors.primary,
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onEvent(SiteManagementEvent.AddDomainClicked) },
                        enabled = state.isSiteManagementActive,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Icon(AppIcons.Add, contentDescription = "افزودن", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // The List of Blocked Domains
                Text(
                    text = "لیست سایت‌های مسدود شده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.blockedDomains, key = { it.id }) { domain ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 15.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = domain.domain,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }

                                Switch(
                                    checked = domain.isActive, onCheckedChange = {
                                        onEvent(
                                            SiteManagementEvent.ToggleDomainStatus(
                                                domain, it
                                            )
                                        )
                                    }, enabled = state.isSiteManagementActive
                                )

                                IconButton(
                                    onClick = {
                                        onEvent(
                                            SiteManagementEvent.RemoveDomainClicked(
                                                domain
                                            )
                                        )
                                    }, enabled = state.isSiteManagementActive
                                ) {
                                    Icon(
                                        AppIcons.Close,
                                        contentDescription = "حذف",
                                        tint = colors.red
                                    ) // TODO: change icon to delete
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "1. Site Management (Active with Data)", locale = "fa")
@Composable
fun SiteManagementActivePreview() {
    ParentControlTheme {
        SiteManagementContent(
            state = SiteManagementState(
                childId = "mock-123",
                isSiteManagementActive = true,
                domainInput = "google.com",
                blockedDomains = listOf(
                    BlockedDomainEntity(
                        childId = "mock-123", domain = "badwebsite.com", isActive = true
                    ),
                    BlockedDomainEntity(
                        childId = "mock-123", domain = "casino.com", isActive = false
                    ),
                    BlockedDomainEntity(childId = "mock-123", domain = "games.com", isActive = true)
                )
            ), onEvent = {})
    }
}

@Preview(showBackground = true, name = "2. Site Management (Deactivated)", locale = "fa")
@Composable
fun SiteManagementDeactivatedPreview() {
    ParentControlTheme {
        SiteManagementContent(
            state = SiteManagementState(
                childId = "mock-123",
                isSiteManagementActive = false,
                domainInput = "",
                blockedDomains = emptyList()
            ), onEvent = {})
    }
}