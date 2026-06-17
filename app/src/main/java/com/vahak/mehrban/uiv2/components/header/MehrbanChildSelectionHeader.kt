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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.Gender
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun MehrbanChildSelectionHeader(
    title: String,
    subtitle: String,
    childName: String?,
    childGender: Gender?,
    changeButtonText: String,
    onBackClick: () -> Unit,
    onChangeChildClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val headerGradient = Brush.linearGradient(listOf(colors.primary, colors.primaryVariant))
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = headerGradient,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(top = 40.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = subtitle, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        AppIcons.Back,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { scaleX = if (isRtl) -1f else 1f }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onChangeChildClick() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.cardInnerBG, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (childGender == Gender.BOY) "👦" else "👧", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.child_settings_header_child_label),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Text(
                        text = childName ?: "...",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = changeButtonText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "1. RTL - Child Selection Header")
@Composable
fun PreviewChildSelectionHeader() {
    ParentControlTheme {
        Column {
            MehrbanChildSelectionHeader(
                title = "تنظیمات فرزند",
                subtitle = "مدیریت قوانین",
                childName = "امیرعلی",
                childGender = Gender.BOY,
                changeButtonText = "تغییر",
                onBackClick = {},
                onChangeChildClick = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            MehrbanChildSelectionHeader(
                title = "گزارش فعالیت",
                subtitle = "آمار استفاده",
                childName = "فاطمه",
                childGender = Gender.GIRL,
                changeButtonText = "تغییر فرزند",
                onBackClick = {},
                onChangeChildClick = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "en", name = "2. LTR - Child Selection Header")
@Composable
fun PreviewChildSelectionHeaderEnglish() {
    ParentControlTheme {
        Column {
            MehrbanChildSelectionHeader(
                title = "Child settings",
                subtitle = "Manage rules",
                childName = "AmirAli",
                childGender = Gender.BOY,
                changeButtonText = "Change",
                onBackClick = {},
                onChangeChildClick = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            MehrbanChildSelectionHeader(
                title = "Activity report",
                subtitle = "Usage statistics",
                childName = "Fatemeh",
                childGender = Gender.GIRL,
                changeButtonText = "Change child",
                onBackClick = {},
                onChangeChildClick = {}
            )
        }
    }
}