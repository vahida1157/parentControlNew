package com.vahak.mehrban.uiv2.components.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun FilterModeCard(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Card(
        modifier = modifier
            .clickable { onClick() }
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) selectedColor else colors.divider,
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) selectedColor.copy(alpha = 0.1f) else colors.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) selectedColor else colors.textPrimary,
                fontSize = 14.sp
            )
            Text(desc, color = colors.textSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
fun SettingMenuRow(emoji: String, title: String, desc: String, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(colors.cardInnerBG, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(desc, fontSize = 12.sp, color = colors.textSecondary)
            }
            Icon(AppIcons.ChevronLeft, contentDescription = null, tint = colors.textHint)
        }
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewBrowserSharedComponents() {
    ParentControlTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FilterModeCard(
                title = "لیست سفید",
                desc = "فقط سایت‌های مجاز",
                isSelected = true,
                selectedColor = LocalCustomColors.current.green,
                onClick = {}
            )
            Spacer(modifier = Modifier.height(16.dp))
            SettingMenuRow(
                emoji = "✅",
                title = "سایت‌های مجاز",
                desc = "۱ سایت مجاز است",
                onClick = {}
            )
        }
    }
}