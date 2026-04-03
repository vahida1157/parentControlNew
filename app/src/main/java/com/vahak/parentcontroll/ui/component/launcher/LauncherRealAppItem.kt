package com.vahak.parentcontroll.ui.component.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.vahak.parentcontroll.core.util.AppInfo
import com.vahak.parentcontroll.ui.theme.LocalCustomColors

@Composable
fun LauncherRealAppItem(
    modifier: Modifier = Modifier,
    appInfo: AppInfo,
    onClick: (String) -> Unit
) {
    val colors = LocalCustomColors.current

    // Safely convert the Android Drawable to a Jetpack Compose ImageBitmap
    // We use 'remember' so it only does this heavy conversion once per icon
    val imageBitmap = remember(appInfo.icon) {
        appInfo.icon.toBitmap().asImageBitmap()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick(appInfo.packageName) }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(Color.White, RoundedCornerShape(20.dp)), // White background makes real icons pop
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = appInfo.name,
                modifier = Modifier.size(50.dp) // Leave a nice white border around the actual icon
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = appInfo.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis // Adds "..." if an app name is extremely long
        )
    }
}