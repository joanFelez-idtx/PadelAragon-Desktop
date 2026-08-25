package com.padelaragon.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = NavyDark,
    primaryContainer = BlueLight,
    onPrimaryContainer = NavyDark,
    secondary = BlueSecondary,
    onSecondary = NavyDark,
    secondaryContainer = BlueLighter,
    onSecondaryContainer = NavyMedium,
    tertiary = BlueTertiary,
    onTertiary = PureWhite,
    tertiaryContainer = BlueLightest,
    onTertiaryContainer = NavyDark,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark,
    background = PureWhite,
    onBackground = NavyDark,
    surface = BlueSurface,
    onSurface = NavyDark,
    surfaceVariant = BlueSecondary,
    onSurfaceVariant = NavyMedium,
    outline = NavySoft
)

/**
 * Desktop version of the app theme. Status-bar/window-chrome coloring from the
 * Android version doesn't apply on desktop, so this only sets the Material3
 * color scheme and typography.
 */
@Composable
fun PadelAragonTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
