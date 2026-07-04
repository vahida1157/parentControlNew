package com.vahak.mehrban.uiv2.components.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun SettingsListItemCard(
    title: String,
    subtitle: String? = null,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                }
            }
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(AppIcons.Edit, contentDescription = "Edit", tint = colors.textSecondary)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(AppIcons.DeleteForever, contentDescription = "Delete", tint = colors.red)
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewSettingsListItemCard() {
    ParentControlTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SettingsListItemCard(
                title = "وبسایت آپارات",
                subtitle = "aparat.com",
                onEditClick = {},
                onDeleteClick = {}
            )
            SettingsListItemCard(
                title = "aparat.com",
                subtitle = null,
                onEditClick = {},
                onDeleteClick = {}
            )
        }
    }
}