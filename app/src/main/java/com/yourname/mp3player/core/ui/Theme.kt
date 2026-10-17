package com.yourname.mp3player.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.yourname.mp3player.domain.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB69CFF), onPrimary = Color(0xFF21104F),
    primaryContainer = Color(0xFF4A32A8), onPrimaryContainer = Color(0xFFE9E0FF),
    secondary = Color(0xFFFF8CC0), onSecondary = Color(0xFF4A0E2E),
    secondaryContainer = Color(0xFF7A2B57), onSecondaryContainer = Color(0xFFFFD9E8),
    background = Color(0xFF0E0B1A), onBackground = Color(0xFFEDEAF7),
    surface = Color(0xFF0E0B1A), onSurface = Color(0xFFEDEAF7),
    surfaceVariant = Color(0xFF2A2547), onSurfaceVariant = Color(0xFFBDB7D6),
    surfaceContainer = Color(0xFF181430),
    surfaceContainerHigh = Color(0xFF221D3E),
    surfaceContainerHighest = Color(0xFF2D2750),
    outline = Color(0xFF7D769E)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6D4AE0), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DDFF), onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFFC2306F), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E8), onSecondaryContainer = Color(0xFF3F0020),
    background = Color(0xFFFAF8FF), onBackground = Color(0xFF1B1830),
    surface = Color(0xFFFAF8FF), onSurface = Color(0xFF1B1830),
    surfaceVariant = Color(0xFFE7E1F5), onSurfaceVariant = Color(0xFF494560),
    surfaceContainer = Color(0xFFF0ECFB),
    surfaceContainerHigh = Color(0xFFE9E3F8),
    surfaceContainerHighest = Color(0xFFE2DBF5),
    outline = Color(0xFF7A7592)
)

private val AmoledColors = DarkColors.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainer = Color(0xFF0A0812),
    surfaceContainerHigh = Color(0xFF15111F),
    surfaceContainerHighest = Color(0xFF1E1930)
)

@Composable
fun AppTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val scheme = when (mode) {
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) DarkColors else LightColors
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
        ThemeMode.AMOLED -> AmoledColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
