package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors

@Composable
fun ChildSelectorCard(
    activeChild: ChildEntity?,
    otherChildren: List<ChildEntity>,
    onAddClick: () -> Unit,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Side: Active Child
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(colors.blue.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(AppIcons.YoungChild, contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(15.dp))
                Text(
                    text = activeChild?.name ?: "افزودن فرزند",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            // Left Side: Mini Avatars & Add Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-12).dp) // Overlapping effect
            ) {
                // Other Children
                otherChildren.take(2).forEachIndexed { index, _ ->
                    val tintColor = if (index == 0) colors.yellow else colors.green
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(tintColor.copy(alpha = 0.2f), CircleShape)
                            .border(2.dp, colors.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.YoungChild, contentDescription = null, tint = tintColor, modifier = Modifier.size(18.dp))
                    }
                }

                // Add Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.background, CircleShape)
                        .border(2.dp, colors.surface, CircleShape)
                        .clickable { onAddClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(AppIcons.Add, contentDescription = null, tint = colors.textHint, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}