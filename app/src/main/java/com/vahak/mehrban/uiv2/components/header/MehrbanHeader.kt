package com.vahak.mehrban.uiv2.components.header

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

sealed class HeaderAction {
    data class Back(val onClick: () -> Unit) : HeaderAction()
    data class Profile(val onClick: () -> Unit) : HeaderAction()
    data class Add(val onClick: () -> Unit) : HeaderAction()
    object None : HeaderAction()
}

@Composable
fun MehrbanHeader(
    title: String,
    subtitle: String? = null,
    iconEmoji: String? = null,
    action: HeaderAction = HeaderAction.None,
    bottomPadding: Dp = 40.dp,
    cornerRadius: Dp = 24.dp
) {
    val colors = LocalCustomColors.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val headerGradient = Brush.linearGradient(listOf(colors.primary, colors.primaryVariant))
    val avatarGradient = Brush.linearGradient(listOf(colors.yellow, colors.orange))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = headerGradient,
                shape = RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
            )
            .padding(top = 40.dp, bottom = bottomPadding, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconEmoji != null) {
                    Text(iconEmoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Action Button Rendering based on Sealed Class
            when (action) {
                is HeaderAction.Back -> {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { action.onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AppIcons.Back,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.graphicsLayer {
                                scaleX = if (isRtl) -1f else 1f // 🚀 Automatically mirror arrow
                            })
                    }
                }

                is HeaderAction.Profile -> {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(avatarGradient, CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .clip(CircleShape)
                            .clickable { action.onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AppIcons.Profile,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                is HeaderAction.Add -> {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .clip(CircleShape)
                            .clickable { action.onClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            AppIcons.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                is HeaderAction.None -> { /* Draw Nothing */
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "1. RTL (Persian) - General Headers")
@Composable
fun PreviewMehrbanHeaders() {
    ParentControlTheme {
        Column {
            // Dashboard Style (Profile Action)
            MehrbanHeader(
                title = "والد عزیز",
                subtitle = "سلام، وقت بخیر",
                action = HeaderAction.Profile(onClick = {})
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Family Screen Style (Add Action)
            MehrbanHeader(
                title = "فرزندان شما",
                subtitle = "مدیریت فرزندان",
                action = HeaderAction.Add(onClick = {})
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Selection Style (Back Action + Emoji)
            MehrbanHeader(
                title = "مدیریت برنامه‌ها",
                subtitle = "انتخاب برنامه‌های مجاز",
                iconEmoji = "📱",
                action = HeaderAction.Back(onClick = {})
            )
        }
    }
}

@Preview(
    showBackground = true,
    locale = "en",
    name = "2. LTR (English) - General Headers"
)
@Composable
fun PreviewMehrbanHeadersEnglish() {
    ParentControlTheme {
        Column {
            // Dashboard Style (Profile Action)
            MehrbanHeader(
                title = "Dear Parent",
                subtitle = "Hello, good to see you",
                action = HeaderAction.Profile(onClick = {})
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Family Screen Style (Add Action)
            MehrbanHeader(
                title = "Your children",
                subtitle = "Manage children",
                action = HeaderAction.Add(onClick = {})
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App Selection Style (Back Action + Emoji)
            MehrbanHeader(
                title = "App Management",
                subtitle = "Select allowed apps",
                iconEmoji = "📱",
                action = HeaderAction.Back(onClick = {})
            )
        }
    }
}