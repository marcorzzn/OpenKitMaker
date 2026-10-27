// Implements: Material 3 Theme definition aligned with shared/design/tokens.json
package com.openkitmaker.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OpenKitDarkColors = darkColorScheme(
    primary = OpenKitPrimary,
    secondary = OpenKitSecondary,
    background = OpenKitBackground,
    surface = OpenKitSurface,
    surfaceVariant = OpenKitSurfaceElevated,
    outline = OpenKitSurfaceBorder,
    onPrimary = Color.White,
    onSecondary = OpenKitBackground,
    onBackground = OpenKitTextPrimary,
    onSurface = OpenKitTextPrimary,
    error = OpenKitError
)

@Composable
fun OpenKitTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = OpenKitDarkColors,
        typography = OpenKitTypography,
        content = content
    )
}
