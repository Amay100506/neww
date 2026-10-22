package com.priosort.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandPink,
    onPrimary = Ink,
    secondary = BrandPinkSoft,
    onSecondary = Ink,
    tertiary = BrandPinkSoft,
    background = Ink,
    onBackground = Color.White,
    surface = CardDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF22222B),
    onSurfaceVariant = TextMuted,
    outline = Color(0xFF464651)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFC51B67),
    onPrimary = Color.White,
    secondary = Color(0xFF9E2B5B),
    tertiary = BrandPink,
    background = LightBackground,
    onBackground = Color(0xFF21141B),
    surface = LightSurface,
    onSurface = Color(0xFF21141B),
    surfaceVariant = Color(0xFFF5E8EE),
    onSurfaceVariant = Color(0xFF63535C)
)

@Composable
fun PrioSortTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
