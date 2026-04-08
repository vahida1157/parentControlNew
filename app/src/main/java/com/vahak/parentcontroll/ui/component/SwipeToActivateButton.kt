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
    isActive: Boolean = false,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
) {
    val colors = LocalCustomColors.current

    // State to track drag ONLY. Activation is handled by the parent!
    var offsetX by remember { mutableFloatStateOf(0f) }

    // Constants
    val height = 64.dp
    val thumbSize = 52.dp
    val padding = 6.dp

    // Convert dp to px for logic
    val density = LocalDensity.current
    val maxDragPx = with(density) { 250.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(0xFFE8ECEF), RoundedCornerShape(32.dp))
            .padding(padding)
    ) {
        // Background Text
        if (!isActive) {
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
                .offset { IntOffset(if (isActive) 0 else offsetX.roundToInt(), 0) }
                .then(if (isActive) Modifier.fillMaxSize() else Modifier.size(thumbSize))
                .clip(RoundedCornerShape(32.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(colors.primary, colors.secondary)
                    )
                )
                .then(
                    if (isActive) {
                        Modifier.clickable {
                            offsetX = 0f
                            onDeactivate()
                        }
                    } else {
                        Modifier.draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                val newOffset = offsetX - delta
                                offsetX = newOffset.coerceIn(0f, maxDragPx)
                            },
                            onDragStopped = {
                                if (offsetX > maxDragPx * 0.7) {
                                    offsetX = 0f
                                    onActivate() // Fire to parent!
                                } else {
                                    offsetX = 0f
                                }
                            }
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                // Activated View
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.slider_activated) + " (توقف)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(AppIcons.Check, contentDescription = null, tint = Color.White)
                }
            } else {
                // Not Activated View
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
                isActive = false,
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
                isActive = true,
                onActivate = {},
                onDeactivate = {}
            )
        }
    }
}