package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun SimpleCurvedHeader(
    title: String,
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                brush = Brush.linearGradient(listOf(colors.primary, colors.secondary)),
                shape = RoundedCornerShape(bottomStart = 45.dp, bottomEnd = 45.dp)
            )
            .padding(top = 40.dp, start = 25.dp, end = 25.dp)
    ) {
        Icon(
            painter = AppIcons.Back,
            contentDescription = "Back",
            tint = colors.surface,
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.TopEnd)
                .clickable { onBackClick() }
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.surface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Preview(showBackground = true, name = "1. Curved Header Light", locale = "fa")
@Composable
fun SimpleCurvedHeaderPreviewLight() {
    ParentControlTheme(darkTheme = false) {
        SimpleCurvedHeader(
            title = "افزودن فرزند جدید",
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Curved Header Dark", locale = "fa")
@Composable
fun SimpleCurvedHeaderPreviewDark() {
    ParentControlTheme(darkTheme = true) {
        SimpleCurvedHeader(
            title = "افزودن فرزند جدید",
            onBackClick = {}
        )
    }
}