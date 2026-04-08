package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun AppToggleItem(
    appName: String,
    packageName: String,
    isAllowed: Boolean,
    iconBitmap: ImageBitmap? = null, // ADDED THIS PARAMETER!
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalCustomColors.current

    Card(
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(1.dp, colors.divider, RoundedCornerShape(15.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = "$appName icon",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        painter = AppIcons.ChartPie,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(15.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appName,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = packageName,
                    color = colors.textHint,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
                checked = isAllowed,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.surface,
                    checkedTrackColor = colors.primary,
                    uncheckedThumbColor = colors.textHint,
                    uncheckedTrackColor = colors.divider
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "App toggle item", locale = "fa")
@Composable
fun AppToggleItemPreview() {
    ParentControlTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            AppToggleItem(
                appName = "Youtube",
                packageName = "com.google.android.youtube",
                isAllowed = true,
                iconBitmap = null, // Safe for preview
                onToggle = {}
            )
        }
    }
}