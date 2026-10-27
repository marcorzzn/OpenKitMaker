// Implements: Design System Tokens from shared/design/tokens.json
package com.openkitmaker.android.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * OpenKitMaker Design System Color Tokens.
 * Single source of truth: shared/design/tokens.json
 */
val OpenKitPrimary = Color(0xFF0F52BA)
val OpenKitPrimaryVariant = Color(0xFF0A387E)
val OpenKitSecondary = Color(0xFF00C49F)
val OpenKitSecondaryVariant = Color(0xFF009E7E)
val OpenKitBackground = Color(0xFF0F141C)
val OpenKitSurface = Color(0xFF1A2230)
val OpenKitSurfaceElevated = Color(0xFF253146)
val OpenKitSurfaceBorder = Color(0xFF2E3D56)
val OpenKitTextPrimary = Color(0xFFFFFFFF)
val OpenKitTextSecondary = Color(0xFF94A3B8)
val OpenKitTextTertiary = Color(0xFF64748B)
val OpenKitError = Color(0xFFEF4444)
val OpenKitSuccess = Color(0xFF10B981)
val OpenKitWarning = Color(0xFFF59E0B)
val OpenKitAccent = Color(0xFFFFB800)

/**
 * OpenKitMaker Spacing Tokens.
 */
object OpenKitSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/**
 * OpenKitMaker Corner Radii Tokens.
 */
object OpenKitRadii {
    val none = 0.dp
    val sm = 4.dp
    val md = 8.dp
    val lg = 16.dp
    val xl = 24.dp
    val full = 9999.dp
}
