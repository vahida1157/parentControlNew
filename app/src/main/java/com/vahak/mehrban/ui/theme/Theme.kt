package com.vahak.mehrban.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private fun LightColors.toCustomColors() = CustomColors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    background = background,
    surface = surface,
    cardInnerBG = cardInnerBG,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    textHint = textHint,
    divider = divider,
    bottomNavigationBG = bottomNavigationBG,
    iconColor = iconColor,
    green = green,
    greenLight = greenLight,
    red = red,
    redLight = redLight,
    blue = blue,
    yellow = yellow,
    orange = orange,
    orangeLight = orangeLight,
    indicatorSelected = indicatorSelected,
    indicatorUnSelected = indicatorUnSelected,
    backgroundCardTeal = backgroundCardTeal,
    backgroundCardBlue = backgroundCardBlue,
    backgroundCardYellow = backgroundCardYellow,
    backgroundCardRed = backgroundCardRed,
    backgroundButtonDisable = backgroundButtonDisable,
)

private fun DarkColors.toCustomColors() = CustomColors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    background = background,
    surface = surface,
    cardInnerBG = cardInnerBG,
    textPrimary = textPrimary,
    textSecondary = textSecondary,
    textHint = textHint,
    divider = divider,
    bottomNavigationBG = bottomNavigationBG,
    iconColor = iconColor,
    green = green,
    greenLight = greenLight,
    red = red,
    redLight = redLight,
    blue = blue,
    yellow = yellow,
    orange = orange,
    orangeLight = orangeLight,
    indicatorSelected = indicatorSelected,
    indicatorUnSelected = indicatorUnSelected,
    backgroundCardTeal = backgroundCardTeal,
    backgroundCardBlue = backgroundCardBlue,
    backgroundCardYellow = backgroundCardYellow,
    backgroundCardRed = backgroundCardRed,
    backgroundButtonDisable = backgroundButtonDisable,
)


val LocalCustomColors = staticCompositionLocalOf<CustomColors> {
    error("No CustomColors provided")
}

val LocalFontScale = staticCompositionLocalOf { mutableFloatStateOf(1f) }


@Composable
fun ParentControlTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true, content: @Composable () -> Unit
) {
    val colorTheme =
        if (darkTheme) DarkColors().toCustomColors() else LightColors().toCustomColors()
    val fontScale = LocalFontScale.current
    CompositionLocalProvider(
        LocalCustomColors provides colorTheme,
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalFontScale provides fontScale
    ) {
        MaterialTheme(
            typography = dynamicTypography(fontScale.floatValue), content = content
        )
    }
}