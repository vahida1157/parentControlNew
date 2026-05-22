package com.vahak.mehrban.ui.component.launcher

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme

@Composable
fun LauncherBottomDock(
    modifier: Modifier = Modifier,
    onLeftIconClick: () -> Unit,
    onRightIconClick: () -> Unit,
    onCenterDrawerClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    // The main Box allows us to overlap the floating button over the dock card
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. The White Background Card
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp, vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Icon (e.g., Gallery)
                Icon(
                    painter = AppIcons.Gallery, // Replace with AppIcons.Gallery
                    contentDescription = "Gallery",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { onLeftIconClick() }
                )

                // Empty space so the center button can sit here
                Spacer(modifier = Modifier.width(60.dp))

                // Right Icon (e.g., Call)
                Icon(
                    painter = AppIcons.Phone, // Replace with AppIcons.Phone
                    contentDescription = "Call",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable { onRightIconClick() }
                )
            }
        }

        // 2. The Floating Center Button (App Drawer)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-10).dp) // Pulls it slightly up over the card edge
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primary), // Your gradient green
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .size(65.dp)
                    // The CSS border: 4px solid var(--bg-body) to cut out the white card behind it
                    .border(4.dp, colors.background, RoundedCornerShape(24.dp))
                    .clickable { onCenterDrawerClick() }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        painter = AppIcons.Apps, // Replace with AppIcons.Apps
                        contentDescription = "App Drawer",
                        tint = Color.White, // FIXME: white is wrong for dark mode
                        modifier = Modifier.size(35.dp)
                    )
                }
            }

            // The tiny label underneath the floating button
            Text(
                text = "برنامه‌های مجاز",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "Launcher Bottom Dock", locale = "fa")
@Composable
fun LauncherBottomDockPreview() {
    ParentControlTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp) // Push it down for preview purposes
        ) {
            LauncherBottomDock(
                onLeftIconClick = {},
                onRightIconClick = {},
                onCenterDrawerClick = {}
            )
        }
    }
}