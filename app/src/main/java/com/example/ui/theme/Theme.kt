package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ChampagneBright,
    onPrimary = EmeraldDarkSurface,
    primaryContainer = EmeraldRich,
    onPrimaryContainer = EmeraldSoft,
    secondary = TurquoiseAccent,
    onSecondary = Color.White,
    secondaryContainer = EmeraldDarkElevated,
    onSecondaryContainer = LightInk,
    tertiary = ChampagneGold,
    onTertiary = EmeraldDarkSurface,
    background = EmeraldDarkSurface,
    onBackground = LightInk,
    surface = EmeraldDarkCard,
    onSurface = LightInk,
    surfaceVariant = EmeraldDarkElevated,
    onSurfaceVariant = LightInkMuted,
    outline = Color(0xFF2D4B45),
    error = Color(0xFFFF8A80)
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = EmeraldDeep,
    secondary = ChampagneGold,
    onSecondary = InkPrimary,
    secondaryContainer = ChampagneLight,
    onSecondaryContainer = Color(0xFF4A380E),
    tertiary = TurquoiseAccent,
    onTertiary = Color.White,
    tertiaryContainer = TurquoiseContainer,
    onTertiaryContainer = Color(0xFF0A3D44),
    background = IvoryBackground,
    onBackground = InkPrimary,
    surface = IvorySurface,
    onSurface = InkPrimary,
    surfaceVariant = IvoryVariant,
    onSurfaceVariant = InkSecondary,
    outline = Color(0xFFC5D0CC),
    error = ErrorCrimson
)

val ZarBankShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ZarBankTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ZarBankTypography,
            shapes = ZarBankShapes,
            content = content
        )
    }
}
