package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    modifier: Modifier = Modifier,
    isInitiallyActivated: Boolean = false,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
) {
    val colors = LocalCustomColors.current

    // State to track drag and activation
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isActivated by remember { mutableStateOf(isInitiallyActivated) }

    // Constants
    val height = 64.dp
    val thumbSize = 52.dp
    val padding = 6.dp

    // Convert dp to px for logic
    val density = LocalDensity.current
    val maxDragPx = with(density) { 250.dp.toPx() } // Approximate width available minus thumb

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(0xFFE8ECEF), RoundedCornerShape(32.dp))
            .padding(padding)
    ) {
        // Background Text (Only needed when not activated, since thumb covers it otherwise)
        if (!isActivated) {
            Text(
                text = stringResource(R.string.slider_instruction),
                modifier = Modifier.align(Alignment.Center),
                color = colors.textSecondary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Draggable / Clickable Thumb
        Box(
            modifier = Modifier
                // 1. Position: If activated, stay at 0. If not, follow the drag offset.
                .offset { IntOffset(if (isActivated) 0 else offsetX.roundToInt(), 0) }
                // 2. Size: If activated, fill the whole bar. If not, stay a circle.
                .then(if (isActivated) Modifier.fillMaxSize() else Modifier.size(thumbSize))
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(colors.primary, colors.secondary)
                    )
                )
                // 3. Behavior: If activated, click to turn off. If not, drag to turn on.
                .then(
                    if (isActivated) {
                        Modifier.clickable {
                            isActivated = false
                            offsetX = 0f
                            onDeactivate()
                        }
                    } else {
                        Modifier.draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                // Standard RTL Logic: negative drag implies forward.
                                val newOffset = offsetX - delta
                                offsetX = newOffset.coerceIn(0f, maxDragPx)
                            },
                            onDragStopped = {
                                if (offsetX > maxDragPx * 0.7) {
                                    isActivated = true
                                    offsetX = 0f // Reset offset completely
                                    onActivate()
                                } else {
                                    offsetX = 0f // Snap back if they didn't drag far enough
                                }
                            }
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isActivated) {
                // Activated View (Full width, text inside)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.slider_activated) + " (توقف)", // Added "Stop" to hint it's clickable
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(AppIcons.Check, contentDescription = null, tint = Color.White)
                }
            } else {
                // Not Activated View (Just the arrow)
                Icon(
                    painter = AppIcons.ChevronLeft,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Slider - Deactivated", widthDp = 360, locale = "fa")
@Composable
fun SwipeSliderDeactivatedPreview() {
    ParentControlTheme {
        Box(modifier = Modifier.padding(16.dp).background(Color.White)) {
            SwipeToActivateButton(
                isInitiallyActivated = false,
                onActivate = {},
                onDeactivate = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "2. Slider - Activated", widthDp = 360, locale = "fa")
@Composable
fun SwipeSliderActivatedPreview() {
    ParentControlTheme {
        Box(modifier = Modifier.padding(16.dp).background(Color.White)) {
            SwipeToActivateButton(
                isInitiallyActivated = true, // Forces the green UI to show!
                onActivate = {},
                onDeactivate = {}
            )
        }
    }
}