package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun ListSectionCard(
    title: String,
    headerIcon: Painter,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalCustomColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 25.dp)) {
        // Green Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primary, RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = Color.White, fontWeight = FontWeight.Bold)
            Icon(painter = headerIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        // White Body (Contains the list items)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(bottomStart = 15.dp, bottomEnd = 15.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(bottomStart = 15.dp, bottomEnd = 15.dp))
                .clip(RoundedCornerShape(bottomStart = 15.dp, bottomEnd = 15.dp))
        ) {
            content()
        }
    }
}

@Preview(showBackground = true, name = "Family Section Preview", locale = "fa")
@Composable
fun ChildSectionCardPreview() {
    ParentControlTheme {
        val colors = LocalCustomColors.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background) // Background to show card boundaries
                .padding(16.dp)
        ) {
            ListSectionCard(
                title = "فرزندان",
                headerIcon = AppIcons.YoungChild
            ) {
                // Mocking children inside the card for a realistic preview
                ClickableListItem(
                    name = "محمدمهدی",
                    avatarIcon = AppIcons.YoungChild,
                    avatarTint = colors.yellow,
                    avatarBg = colors.yellow.copy(alpha = 0.2f),
                    showDivider = true
                )
                ClickableListItem(
                    name = "علی",
                    avatarIcon = AppIcons.YoungChild,
                    avatarTint = colors.blue,
                    avatarBg = colors.blue.copy(alpha = 0.2f),
                    showDivider = false // Last item hides divider
                )
            }
        }
    }
}