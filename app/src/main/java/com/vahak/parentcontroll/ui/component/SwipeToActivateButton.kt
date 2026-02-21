package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.vahak.parentcontroll.R
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme
import kotlin.math.roundToInt

@Composable
fun SwipeToActivateButton(
    onActivate: () -> Unit
) {
    val colors = LocalCustomColors.current
    // State to track drag
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isActivated by remember { mutableStateOf(false) }

    // Constants
    val height = 64.dp
    val thumbSize = 52.dp
    val padding = 6.dp

    // Convert dp to px for logic
    val density = LocalDensity.current
    val maxDragPx = with(density) { 250.dp.toPx() } // Approximate width available minus thumb

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(0xFFE8ECEF), RoundedCornerShape(32.dp))
            .padding(padding)
    ) {
        val text = if (isActivated) {
            stringResource(R.string.slider_activated)
        } else {
            stringResource(R.string.slider_instruction)
        }

        // Background Text
        Text(
            text = text,
            modifier = Modifier.align(Alignment.Center),
            color = if (isActivated) colors.primary else colors.textSecondary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )

        // Draggable Thumb
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .size(if (isActivated) 300.dp else thumbSize) // Expand when active
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(colors.primary, colors.secondary)
                    )
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (!isActivated) {
                            val newOffset = offsetX - delta
                            // Standard RTL Logic: Dragging "Left" (negative x) usually usually implies forward in RTL
                            // But for simplicity in Compose 'offset' is physical.
                            // Let's assume physical right drag for now.
                            offsetX = newOffset.coerceIn(0f, maxDragPx)
                        }
                    },
                    onDragStopped = {
                        if (offsetX > maxDragPx * 0.7) {
                            isActivated = true
                            offsetX = maxDragPx // Snap to end (simplified)
                            onActivate()
                        } else {
                            offsetX = 0f // Snap back
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isActivated) {
                Icon(AppIcons.Check, contentDescription = null, tint = Color.White)
            } else {
                Icon(
                    painter = AppIcons.ChevronLeft,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "3. Swipe Slider", widthDp = 360, locale = "fa")
@Composable
fun SwipeSliderPreview() {
    ParentControlTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(Color.White)
        ) {
            SwipeToActivateButton(
                onActivate = {}
            )
        }
    }
}