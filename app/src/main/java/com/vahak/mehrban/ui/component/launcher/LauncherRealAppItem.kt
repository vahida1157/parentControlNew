package com.vahak.mehrban.ui.component.launcher

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.vahak.mehrban.core.util.AppInfo
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme
import android.graphics.Color as AndroidColor

@Composable
fun LauncherRealAppItem(
    modifier: Modifier = Modifier,
    appInfo: AppInfo,
    onClick: (String) -> Unit
) {
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    val density = LocalDensity.current

    // Pro Tip: Convert 72dp to pixels based on the device screen
    val iconSizePx = with(density) { 72.dp.roundToPx() }

    val imageBitmap = remember(appInfo.icon) {
        // Use the calculated px size for the bitmap
        appInfo.icon.toBitmap(width = iconSizePx, height = iconSizePx).asImageBitmap()
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
                .background(
                    Color.White,
                    RoundedCornerShape(20.dp)
                ), // White background makes real icons pop
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

@Preview(showBackground = true, locale = "fa")
@Composable
fun LauncherRealAppItemPreview() {
    ParentControlTheme {
        val dummyDrawable = AndroidColor.RED.toDrawable()

        LauncherRealAppItem(
            appInfo = AppInfo(
                name = "یوتیوب",
                packageName = "com.youtube",
                icon = dummyDrawable
            ),
            onClick = {}
        )
    }
}