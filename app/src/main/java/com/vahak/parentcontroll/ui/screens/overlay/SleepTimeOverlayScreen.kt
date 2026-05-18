package com.vahak.parentcontroll.ui.screens.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahak.parentcontroll.ui.theme.ParentControlTheme
import kotlin.random.Random

@Composable
fun SleepTimeOverlayScreen(onBackClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "sleeptime_animations")

    // 1. Breathing Animation for the Character
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "breathe"
    )

    // 2. Gentle Shake for Badges
    val badgeRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "badge_shake"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2D5C), Color(0xFF4B5296))))
    ) {
        // --- Animated Starry Background ---
        TwinklingStars(infiniteTransition)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // --- The Sleeping Character ---
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer { scaleX = breatheScale; scaleY = breatheScale }
                    .shadow(40.dp, CircleShape, spotColor = Color(0xFFFFD166).copy(alpha = 0.4f))
                    .background(Color(0xFFFFD166), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Floating Zzzs
                ZzzParticle(infiniteTransition, startDelay = 0, offsetX = 50f, offsetY = -60f, fontSize = 24)
                ZzzParticle(infiniteTransition, startDelay = 1000, offsetX = 90f, offsetY = -120f, fontSize = 32)
                ZzzParticle(infiniteTransition, startDelay = 2000, offsetX = 130f, offsetY = -180f, fontSize = 40)

                // Face (Closed Eyes)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(30.dp),
                    modifier = Modifier.offset(y = (-10).dp)
                ) {
                    ClosedEye()
                    ClosedEye()
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- Texts ---
            Text(
                text = "خسته نباشی قهرمان!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "گوشی الان خیلی خسته شده و نیاز داره بخوابه تا باتریش پر بشه.\nوقتشه بری سراغ اسباب‌بازی‌هات یا یه نقاشی قشنگ بکشی!",
                fontSize = 16.sp,
                color = Color(0xFFE2E8F0),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- Gentle Shaking Badges ---
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                SleepTimeBadge("🎨", badgeRotation)
                SleepTimeBadge("🧸", -badgeRotation) // Opposite phase
                SleepTimeBadge("⚽", badgeRotation)
            }
        }

        // --- Back Button (To Launcher) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
                .background(Color.Transparent, RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBackClick() }
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text("برگشت", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        }
    }
}

@Composable
fun ClosedEye() {
    Box(
        modifier = Modifier
            .size(30.dp, 15.dp)
            .border(width = 6.dp, color = Color(0xFF4A4A4A), shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
    )
}

@Composable
fun ZzzParticle(
    infiniteTransition: InfiniteTransition,
    startDelay: Int,
    offsetX: Float,
    offsetY: Float,
    fontSize: Int
) {
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = startDelay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "zzz_progress"
    )

    // Opacity fades in then out
    val alpha = if (progress < 0.5f) progress * 2 else 2f - (progress * 2)
    
    Text(
        text = "Z",
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize.sp,
        modifier = Modifier
            .graphicsLayer {
                translationX = progress * offsetX
                translationY = progress * offsetY
                scaleX = 0.8f + (progress * 0.4f)
                scaleY = 0.8f + (progress * 0.4f)
                this.alpha = alpha
            }
    )
}

@Composable
fun SleepTimeBadge(emoji: String, rotation: Float) {
    Box(
        modifier = Modifier
            .graphicsLayer { rotationZ = rotation }
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 15.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 24.sp)
    }
}

@Composable
fun TwinklingStars(infiniteTransition: InfiniteTransition) {
    val twinkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "twinkle"
    )

    // Generate static random positions for stars
    val stars = remember {
        List(50) {
            Offset(Random.nextFloat(), Random.nextFloat())
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = twinkleAlpha }) {
        stars.forEach { offset ->
            drawCircle(
                color = Color.White,
                radius = Random.nextFloat() * 3f + 1f,
                center = Offset(offset.x * size.width, offset.y * size.height),
                alpha = Random.nextFloat() * 0.5f + 0.3f
            )
        }
    }
}

// ==========================================
// PREVIEW
// ==========================================
@Preview(showBackground = true, locale = "fa", widthDp = 360, heightDp = 800)
@Composable
fun SleepTimeOverlayScreenPreview() {
    ParentControlTheme {
        SleepTimeOverlayScreen(onBackClick = {})
    }
}