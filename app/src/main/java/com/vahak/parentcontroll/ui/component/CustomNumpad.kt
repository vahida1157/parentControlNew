package com.vahak.parentcontroll.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun AnimatedPinDots(
    pinLength: Int = 5,
    currentInputLength: Int,
    isError: Boolean = false
) {
    val colors = LocalCustomColors.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 20.dp)
    ) {
        for (i in 0 until pinLength) {
            val isFilled = i < currentInputLength
            
            // Animations
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> colors.red
                    isFilled -> colors.primary
                    else -> colors.divider
                },
                animationSpec = tween(200)
            )
            
            val scale by animateFloatAsState(
                targetValue = if (isFilled || isError) 1.3f else 1f,
                animationSpec = tween(200)
            )

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .scale(scale)
                    .background(dotColor, CircleShape)
            )
        }
    }
}

@Composable
fun CustomNumpad(
    onNumberClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(25.dp)) {
                row.forEach { number ->
                    NumpadButton(text = number, onClick = { onNumberClick(number) })
                }
            }
        }

        // Bottom Row (Clear, 0, Backspace)
        Row(
            horizontalArrangement = Arrangement.spacedBy(25.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "پاک کردن",
                color = colors.textHint,
                fontSize = 14.sp,
                modifier = Modifier
                    .width(70.dp)
                    .clickable { onClearClick() }
                    .padding(vertical = 10.dp)
            )
            
            NumpadButton(text = "0", onClick = { onNumberClick("0") })
            
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .clickable { onBackspaceClick() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.ChevronLeft, contentDescription = "Back", tint = colors.textSecondary) // Replace with a backspace icon if you have one
            }
        }
    }
}

@Composable
fun NumpadButton(text: String, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    Box(
        modifier = Modifier
            .size(70.dp)
            .background(colors.surface, CircleShape)
            .border(1.dp, colors.divider, CircleShape)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
    }
}

// ==========================================
// PREVIEWS
// ==========================================
@Preview(showBackground = true, locale = "fa")
@Composable
fun PinDotsPreview() {
    ParentControlTheme {
        Column(modifier = Modifier.padding(20.dp)) {
            AnimatedPinDots(currentInputLength = 0)
            AnimatedPinDots(currentInputLength = 3)
            AnimatedPinDots(currentInputLength = 5, isError = true)
        }
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun NumpadPreview() {
    ParentControlTheme {
        Box(modifier = Modifier.padding(20.dp).background(Color(0xFFF0FDF4))) {
            CustomNumpad(onNumberClick = {}, onBackspaceClick = {}, onClearClick = {})
        }
    }
}