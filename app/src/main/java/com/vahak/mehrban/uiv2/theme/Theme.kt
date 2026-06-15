// Theme.kt
package com.vahak.mehrban.uiv2.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private fun LightColorsV2.toCustomColors() = CustomColorsV2(
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
    textOnPrimaryVariant = textOnPrimaryVariant,
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

private fun DarkColorsV2.toCustomColors() = CustomColorsV2(
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
    textOnPrimaryVariant = textOnPrimaryVariant,
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

val LocalCustomColors = staticCompositionLocalOf<CustomColorsV2> {
    error("No CustomColors provided")
}

val LocalFontScale = staticCompositionLocalOf { mutableFloatStateOf(1f) }

@Composable
fun ParentControlTheme(
    themeMode: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    val colorTheme = if (darkTheme) DarkColorsV2().toCustomColors() else LightColorsV2().toCustomColors()
    val fontScale = LocalFontScale.current

    CompositionLocalProvider(
        LocalCustomColors provides colorTheme,
        LocalFontScale provides fontScale
    ) {
        MaterialTheme(
            typography = dynamicTypography(fontScale.floatValue), content = content
        )
    }
}