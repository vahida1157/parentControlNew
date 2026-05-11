package com.vahak.parentcontroll.uiv2.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vahak.parentcontroll.R

val CustomFontBold = FontFamily(Font(R.font.iransans_bold))
val CustomFontMedium = FontFamily(Font(R.font.iransans_medium))
val CustomFontRegular = FontFamily(Font(R.font.iransans_regular))

fun dynamicTypography(scale: Float): Typography {
    return Typography(
        titleLarge = TextStyle(
            fontSize = 22.sp * scale,
            fontFamily = CustomFontBold,
            fontWeight = FontWeight(700)
        ),
        titleMedium = TextStyle(
            fontSize = 16.sp * scale,
            fontFamily = CustomFontBold,
            fontWeight = FontWeight(700)
        ),
        titleSmall = TextStyle(
            fontSize = 14.sp * scale,
            fontFamily = CustomFontBold,
            fontWeight = FontWeight(700)
        ),

        bodyLarge = TextStyle(
            fontSize = 16.sp * scale,
            fontFamily = CustomFontMedium,
            fontWeight = FontWeight(500)
        ),
        bodyMedium = TextStyle(
            fontSize = 14.sp * scale,
            fontFamily = CustomFontMedium,
            fontWeight = FontWeight(500)
        ),
        bodySmall = TextStyle(
            fontSize = 12.sp * scale,
            fontFamily = CustomFontMedium,
            fontWeight = FontWeight(500)
        ),

        labelLarge = TextStyle(
            fontSize = 14.sp * scale,
            fontFamily = CustomFontRegular,
            fontWeight = FontWeight(400)
        ),
        labelMedium = TextStyle(
            fontSize = 12.sp * scale,
            fontFamily = CustomFontRegular,
            fontWeight = FontWeight(400)
        ),
        labelSmall = TextStyle(
            fontSize = 10.sp * scale,
            fontFamily = CustomFontRegular,
            fontWeight = FontWeight(400)
        )
    )
}