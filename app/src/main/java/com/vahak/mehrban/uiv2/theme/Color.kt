// Color.kt
package com.vahak.mehrban.uiv2.theme

import androidx.compose.ui.graphics.Color

// --- New Teal/Industrial Theme Palette from HTML ---
// Light Mode
private val LightBg = Color(0xFFF4EFE6)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceInput = Color(0xFFFAF8F4)
private val LightPrimary = Color(0xFF1A8A7D)
private val LightPrimaryDark = Color(0xFF0F6B60)
private val LightGold = Color(0xFFD4A843)
private val LightCoral = Color(0xFFE8575A)
private val LightIndigo = Color(0xFF5A67D8)
private val LightTextMain = Color(0xFF1F2937)
private val LightTextLight = Color(0xFF6B7280)
private val LightTextMuted = Color(0xFF9CA3AF)
private val LightBorder = Color(0xFFE8E0D4)
private val LightTextOnPrimary = Color(0xFFFFFFFF)

private val LightSuccess = Color(0xFF16A34A)
private val LightSuccessSoft = Color(0xFFE8F5E9)
private val LightDangerSoft = Color(0xFFFCE4EC)

// Dark Mode
private val DarkBg = Color(0xFF0A1410)
private val DarkSurface = Color(0xFF14241E)
private val DarkSurfaceInput = Color(0xFF11201A)
private val DarkPrimary = Color(0xFF2DD4BF)
private val DarkPrimaryDark = Color(0xFF14B8A6)
private val DarkGold = Color(0xFFFBBF24)
private val DarkCoral = Color(0xFFF87171)
private val DarkIndigo = Color(0xFF818CF8)
private val DarkTextMain = Color(0xFFF1F5F4)
private val DarkTextLight = Color(0xFF94A3A0)
private val DarkTextMuted = Color(0xFF6B7873)
private val DarkBorder = Color(0xFF243D33)
private val DarkTextOnPrimary = Color(0xFF062925)

private val DarkSuccess = Color(0xFF34D399)
private val DarkSuccessSoft = Color(0xFF0F2E21) // Solid approximation of rgba(52,211,153,0.14)
private val DarkDangerSoft = Color(0xFF3D1C1E) // Solid approximation of rgba(248,113,113,0.14)

data class CustomColorsV2(
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val cardInnerBG: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    val divider: Color,
    val bottomNavigationBG: Color,
    val iconColor: Color,
    val textOnPrimaryVariant: Color,
    val green: Color,
    val greenLight: Color,
    val red: Color,
    val redLight: Color,
    val blue: Color,
    val yellow: Color,
    val orange: Color,
    val orangeLight: Color,
    val indicatorSelected: Color,
    val indicatorUnSelected: Color,
    val backgroundCardTeal: Color,
    val backgroundCardBlue: Color,
    val backgroundCardYellow: Color,
    val backgroundCardRed: Color,
    val backgroundButtonDisable: Color,
)

internal data class LightColorsV2(
    val primary: Color = LightPrimary,
    val primaryVariant: Color = LightPrimaryDark,
    val secondary: Color = LightGold,
    val background: Color = LightBg,
    val surface: Color = LightSurface,
    val cardInnerBG: Color = LightSurfaceInput,
    val textPrimary: Color = LightTextMain,
    val textSecondary: Color = LightTextLight,
    val textHint: Color = LightTextMuted,
    val divider: Color = LightBorder,
    val bottomNavigationBG: Color = LightSurface,
    val iconColor: Color = LightPrimary,
    val textOnPrimaryVariant: Color = LightTextOnPrimary,

    val green: Color = LightSuccess,
    val greenLight: Color = LightSuccessSoft,
    val red: Color = LightCoral,
    val redLight: Color = LightDangerSoft,
    val blue: Color = LightIndigo,
    val yellow: Color = LightGold,
    val orange: Color = Color(0xFFF39C12),
    val orangeLight: Color = Color(0xFFFFF3E0),
    val indicatorSelected: Color = LightPrimary,
    val indicatorUnSelected: Color = LightBorder,
    val backgroundCardTeal: Color = LightPrimary,
    val backgroundCardBlue: Color = LightIndigo,
    val backgroundCardYellow: Color = LightGold,
    val backgroundCardRed: Color = LightCoral,
    val backgroundButtonDisable: Color = LightTextMuted
)

internal data class DarkColorsV2(
    val primary: Color = DarkPrimary,
    val primaryVariant: Color = DarkPrimaryDark,
    val secondary: Color = DarkGold,
    val background: Color = DarkBg,
    val surface: Color = DarkSurface,
    val cardInnerBG: Color = DarkSurfaceInput,
    val textPrimary: Color = DarkTextMain,
    val textSecondary: Color = DarkTextLight,
    val textHint: Color = DarkTextMuted,
    val divider: Color = DarkBorder,
    val bottomNavigationBG: Color = DarkSurface,
    val iconColor: Color = DarkPrimary,
    val textOnPrimaryVariant: Color = DarkTextOnPrimary,

    val green: Color = DarkSuccess,
    val greenLight: Color = DarkSuccessSoft,
    val red: Color = DarkCoral,
    val redLight: Color = DarkDangerSoft,
    val blue: Color = DarkIndigo,
    val yellow: Color = DarkGold,
    val orange: Color = Color(0xFFFBBF24),
    val orangeLight: Color = Color(0xFF4A3410),
    val indicatorSelected: Color = DarkPrimary,
    val indicatorUnSelected: Color = DarkBorder,
    val backgroundCardTeal: Color = DarkPrimary,
    val backgroundCardBlue: Color = DarkIndigo,
    val backgroundCardYellow: Color = DarkGold,
    val backgroundCardRed: Color = DarkCoral,
    val backgroundButtonDisable: Color = DarkTextMuted
)