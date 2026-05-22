package com.vahak.mehrban.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme
import kotlin.math.abs
import kotlin.math.roundToInt

enum class WheelPickerStyle(val count: Int) {
    ONE(1),
    THREE(3),
    FIVE(5),
}

@Composable
fun NumberPickerColumn(
    modifier: Modifier = Modifier,
    range: IntRange,
    initialValue: Int,
    pickerStyle: WheelPickerStyle = WheelPickerStyle.THREE,
    itemHeightDp: Dp = 40.dp,
    onValueChange: (Int) -> Unit
) {
    val colors = LocalCustomColors.current
    val items = range.toList()

    // Mathematically find the center offset
    val visibleItemsCount = pickerStyle.count
    val halfVisibleItems = visibleItemsCount / 2

    val initialIndex = items.indexOf(initialValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Bulletproof Index Math
    val centerItemIndex by remember {
        derivedStateOf {
            val itemHeightPx =
                listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 1f
            val fraction = listState.firstVisibleItemScrollOffset / itemHeightPx
            val exactCenter = listState.firstVisibleItemIndex + fraction
            exactCenter.roundToInt().coerceIn(0, items.size - 1)
        }
    }

    LaunchedEffect(centerItemIndex) {
        if (centerItemIndex in items.indices) {
            onValueChange(items[centerItemIndex])
        }
    }

    LazyColumn(
        state = listState,
        // Height auto-adjusts based on how many items you want to see!
        modifier = modifier.height(itemHeightDp * visibleItemsCount),
        flingBehavior = flingBehavior,
        // Padding auto-adjusts so the first/last items always reach the dead center!
        contentPadding = PaddingValues(vertical = itemHeightDp * halfVisibleItems),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed(items) { index, value ->
            val isSelected = index == centerItemIndex

            Box(
                modifier = Modifier
                    .height(itemHeightDp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val exactCenter =
                            listState.firstVisibleItemIndex + (listState.firstVisibleItemScrollOffset / size.height)
                        val distance = index - exactCenter

                        // Limit how far the calculation goes based on visible items
                        val maxDistance = halfVisibleItems.toFloat() + 0.5f
                        val clampedDistance = distance.coerceIn(-maxDistance, maxDistance)

                        // Dynamically soften the 3D angles if the wheel is larger (5 items)
                        val rotationMultiplier = if (visibleItemsCount >= 5) 28f else 45f
                        rotationX = -clampedDistance * rotationMultiplier

                        val scaleMultiplier = if (visibleItemsCount >= 5) 0.12f else 0.15f
                        val scale = 1f - (abs(clampedDistance) * scaleMultiplier)
                        scaleX = scale
                        scaleY = scale

                        // Dynamically adjust the fade out so the edges blend perfectly
                        val alphaMultiplier = if (visibleItemsCount >= 5) 0.35f else 0.5f
                        alpha = 1f - (abs(clampedDistance) * alphaMultiplier)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) colors.primary else colors.textSecondary
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "1. Compact 3-Item Wheel", locale = "fa")
@Composable
fun NumberPickerColumn3ItemsPreview() {
    ParentControlTheme {
        Box(
            modifier = Modifier
                .width(100.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            NumberPickerColumn(
                range = 0..59,
                initialValue = 30,
                pickerStyle = WheelPickerStyle.THREE,
                itemHeightDp = 40.dp,
                onValueChange = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "2. Tall 5-Item Wheel", locale = "fa")
@Composable
fun NumberPickerColumn5ItemsPreview() {
    ParentControlTheme {
        Box(
            modifier = Modifier
                .width(100.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            NumberPickerColumn(
                range = 0..59,
                initialValue = 30,
                pickerStyle = WheelPickerStyle.FIVE,
                itemHeightDp = 30.dp,
                onValueChange = {}
            )
        }
    }
}