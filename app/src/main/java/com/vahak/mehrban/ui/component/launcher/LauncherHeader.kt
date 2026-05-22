package com.vahak.mehrban.ui.component.launcher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LauncherHeader(
    modifier: Modifier = Modifier,
    childName: String,
    // You can pass your Jalali date string here later from a utility class
    currentDateText: String = "چهارشنبه، ۵ اسفند"
) {
    val colors = LocalCustomColors.current

    // State to hold the live clock string
    var currentTime by remember { mutableStateOf("") }

    // Live Clock Coroutine
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm", Locale.US)
        while (true) {
            val time = formatter.format(Date())
            // Convert standard digits to Persian digits natively
            val persianTime = time.map { char ->
                if (char.isDigit()) (char.code - '0'.code + '۰'.code).toChar() else char
            }.joinToString("")

            currentTime = persianTime
            delay(1000) // Update every second
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Live Clock
        Text(
            text = currentTime,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            letterSpacing = 2.sp
        )

        // 2. Date
        Text(
            text = currentDateText,
            fontSize = 16.sp,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // 3. Greeting Card
        Card(
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f)),
            elevation = CardDefaults.cardElevation(5.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(35.dp)
                        .background(Color(0xFFE8F5E9), CircleShape), // Soft green background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = AppIcons.YoungChild,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "سلام $childName",
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "Launcher Header", locale = "fa")
@Composable
fun LauncherHeaderPreview() {
    ParentControlTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            LauncherHeader(
                childName = "محمدمهدی"
            )
        }
    }
}