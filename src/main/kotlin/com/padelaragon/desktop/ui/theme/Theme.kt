package com.padelaragon.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = PureWhite,
    primaryContainer = RedLighter,
    onPrimaryContainer = PureBlack,
    secondary = CharcoalDark,
    onSecondary = PureWhite,
    secondaryContainer = OffWhite,
    onSecondaryContainer = CharcoalDark,
    tertiary = RedPrimaryDark,
    onTertiary = PureWhite,
    tertiaryContainer = RedLightest,
    onTertiaryContainer = PureBlack,
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark,
    background = PureWhite,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = OffWhite,
    onSurfaceVariant = CharcoalMedium,
    outline = GraySoftOutline
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
