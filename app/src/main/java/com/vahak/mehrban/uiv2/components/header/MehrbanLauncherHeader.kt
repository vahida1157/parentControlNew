package com.vahak.mehrban.uiv2.components.header

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.Gender
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun MehrbanLauncherHeader(
    childName: String,
    gender: Gender,
    onExitClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "rotate"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .rotate(rotation)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                colors.primary,
                                colors.orange,
                                colors.yellow,
                                colors.primary
                            )
                        ),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.surface, RoundedCornerShape(18.dp))
                    .border(3.dp, colors.surface, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (gender == Gender.BOY) "👦" else "👧", fontSize = 32.sp)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = (-2).dp,
                        y = 2.dp
                    ) // The offset naturally follows Start/End in Compose
                    .size(14.dp)
                    .background(colors.green, CircleShape)
                    .border(2.dp, colors.surface, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.launcher_greeting),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
            Text(
                childName.ifEmpty { stringResource(R.string.launcher_default_child_name) },
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(colors.surface.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(14.dp))
                .clickable { onExitClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("🔓", fontSize = 20.sp)
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "1. RTL - Launcher Header")
@Composable
fun PreviewLauncherHeader() {
    ParentControlTheme {
        Column(modifier = Modifier.background(LocalCustomColors.current.background)) {
            MehrbanLauncherHeader(
                childName = "آراد",
                gender = Gender.BOY,
                onExitClick = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "en", name = "2. LTR - Launcher Header")
@Composable
fun PreviewLauncherHeaderEnglish() {
    ParentControlTheme {
        Column(modifier = Modifier.background(LocalCustomColors.current.background)) {
            MehrbanLauncherHeader(
                childName = "Arad",
                gender = Gender.BOY,
                onExitClick = {}
            )
        }
    }
}