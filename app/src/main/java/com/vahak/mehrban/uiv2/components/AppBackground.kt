package com.vahak.mehrban.uiv2.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import com.vahak.mehrban.R
import com.vahak.mehrban.uiv2.theme.LocalCustomColors

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    gradientColors: List<Color>? = null,
    patternImageRes: Int = R.drawable.bg_pattern,
    patternAlpha: Float = 0.06f,
    patternScale: Float = 2.5f,
    patternRepeatScale: Float = 0.5f,
    rotationDurationMillis: Int = 120000,
    content: @Composable () -> Unit
) {
    val colors = LocalCustomColors.current
    val bgGradient = gradientColors?.let { Brush.linearGradient(it) }
        ?: Brush.linearGradient(
            colors = listOf(colors.primary, colors.primaryVariant, Color(0xFF0A4F46))
        )

    val infiniteTransition = rememberInfiniteTransition(label = "background_rotation")
    val backgroundRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(rotationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_spin"
    )

    val patternBitmap = ImageBitmap.imageResource(id = patternImageRes)
    val originalBrush = remember(patternBitmap) {
        ShaderBrush(
            ImageShader(
                image = patternBitmap,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated
            )
        )
    }

    val scaledBrush = remember(originalBrush, patternRepeatScale) {
        object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val shader = originalBrush.createShader(size)
                val matrix = android.graphics.Matrix().apply {
                    setScale(patternRepeatScale, patternRepeatScale)
                }
                shader.setLocalMatrix(matrix)
                return shader
            }
        }
    }

    Box(modifier = modifier
        .fillMaxSize()
        .background(bgGradient)) {
        // Rotating pattern layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotate(backgroundRotation)
                .scale(patternScale)
                .alpha(patternAlpha)
                .background(scaledBrush)
        )

        // Foreground content
        content()
    }
}