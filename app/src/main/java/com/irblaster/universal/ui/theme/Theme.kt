package com.irblaster.universal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF00E5FF)
val NeonPurple = Color(0xFFAA00FF)
val NeonGreen = Color(0xFF00FF9D)
val DarkBg = Color(0xFF0A0A0F)
val DarkSurface = Color(0xFF12121A)
val DarkCard = Color(0xFF1A1A28)
val GlowBlue = Color(0xFF2979FF)
val AccentOrange = Color(0xFFFF6D00)

private val DarkColors = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003344),
    onPrimaryContainer = NeonCyan,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2A0044),
    onSecondaryContainer = Color(0xFFDDB0FF),
    tertiary = NeonGreen,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkCard,
    onSurfaceVariant = Color(0xFFBBBBCC),
    error = Color(0xFFFF5252),
    onError = Color.Black,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006688),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8E8FF),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = Color(0xFF6200EE),
    onSecondary = Color.White,
    background = Color(0xFFF0F4F8),
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
)

@Composable
fun IRBlasterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = IRTypography,
        content = content
    )
}
