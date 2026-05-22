package com.vahak.mehrban.ui.screens.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahak.mehrban.ui.theme.ParentControlTheme

@Composable
fun DizzyPhoneScreen(onBackClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "dizzy_animations")

    // 1. Phone Wobble Animation
    val phoneRotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "phone_wobble"
    )

    // 2. Eye Spin Animation
    val eyeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "eye_spin"
    )

    // 3. Bounce Animation for Badges
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "badge_bounce"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFFBAE6FD))))
    ) {
        // --- Floating Clouds (Static for simplicity, but you can animate offset if desired) ---
        Box(modifier = Modifier.offset(x = 50.dp, y = 100.dp).size(120.dp, 40.dp).background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(50)))
        Box(modifier = Modifier.offset(x = 250.dp, y = 250.dp).size(80.dp, 30.dp).background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(50)))

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // --- The Dizzy Phone Character ---
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = phoneRotation
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f) // Rotate from bottom
                    }
                    .size(140.dp, 220.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(25.dp))
                    .border(8.dp, Color(0xFF334155), RoundedCornerShape(25.dp))
                    .shadow(15.dp, RoundedCornerShape(25.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Orbiting Stars
                Text(
                    text = "💫",
                    fontSize = 24.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-30).dp)
                        .graphicsLayer { rotationZ = -eyeRotation } // Spin opposite to eyes
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Eyes
                    Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                        DizzyEye(rotation = eyeRotation)
                        DizzyEye(rotation = -eyeRotation) // Spin opposite direction!
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                    // Mouth
                    Box(
                        modifier = Modifier
                            .size(30.dp, 15.dp)
                            .graphicsLayer { rotationZ = -15f }
                            .border(width = 4.dp, color = Color(0xFF334155), shape = RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                    )
                }

                // Home Indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .size(30.dp, 8.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- Texts ---
            Text(
                text = "وای! سرم گیج رفت! 😵‍💫",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "از بس تو صفحه‌ام تند تند چرخیدی، چشمهام شبیه فرفره شده!\nبیا یه معامله کنیم: تو برو یه کم بازی‌های واقعی بکن، منم اینجا چشمامو می‌بندم تا حالم بیاد سر جاش!",
                fontSize = 16.sp,
                color = Color(0xFF334155),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(15.dp))
                    .padding(15.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // --- Bouncing Badges ---
            Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                ActivityBadge("🏃‍♂️", bounceY)
                ActivityBadge("🧩", bounceY * 0.5f) // Slightly out of sync
                ActivityBadge("⚽", bounceY * 0.8f)
            }
        }

        // --- Back Button (Changed from Parent Settings) ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .clickable { onBackClick() }
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text("برگشت", color = Color(0xFF475569), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DizzyEye(rotation: Float) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { rotationZ = rotation }
            .background(Brush.sweepGradient(listOf(Color(0xFFF8FAFC), Color(0xFFDBEAFE), Color(0xFFF8FAFC))), CircleShape)
            .border(4.dp, Color(0xFF3B82F6), CircleShape)
    )
}

@Composable
fun ActivityBadge(emoji: String, bounceOffset: Float) {
    Box(
        modifier = Modifier
            .graphicsLayer { translationY = bounceOffset }
            .size(60.dp)
            .background(Color.White, CircleShape)
            .border(2.dp, Color(0xFFE2E8F0), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = 28.sp)
    }
}

// ==========================================
// PREVIEWS
// ==========================================
@Preview(showBackground = true, locale = "fa", widthDp = 360, heightDp = 800)
@Composable
fun DizzyPhoneScreenPreview() {
    ParentControlTheme {
        DizzyPhoneScreen(onBackClick = {})
    }
}