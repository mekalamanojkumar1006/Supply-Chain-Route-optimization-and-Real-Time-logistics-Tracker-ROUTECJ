package com.routecj.admin.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Material Design 3 Color Palette for RouteCJ Admin Application.
 * Premium Logistics Theme.
 */

// Brand Colors
val Primary = Color(0xFF00CFC8)           // RouteCJ Cyan
val PrimaryDark = Color(0xFF00B5AF)
val PrimaryLight = Color(0xFF66E2DE)
val Background = Color(0xFFF8FAFC)        // Enterprise Light Surface
val BackgroundDark = Color(0xFF0F172A)
val Secondary = Color(0xFF0F172A)         // Deep Navy
val SecondaryLight = Color(0xFF1E293B)
val SecondaryDark = Color(0xFF020617)

// Tertiary Colors (RouteCJ Blue palette)
val Tertiary = Color(0xFF0096FF)           // RouteCJ Blue
val TertiaryLight = Color(0xFF60A5FA)
val TertiaryDark = Color(0xFF1D4ED8)

// Status Colors
val Success = Color(0xFF22C55E)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)

// Neutral Colors
val Surface = Color(0xFFFFFFFF)           // Crisp White Surfaces
val SurfaceDark = Color(0xFF1E293B)
val TextPrimary = Color(0xFF0F172A)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF475569)     // High contrast text secondary
val TextSecondaryDark = Color(0xFFCBD5E1)
val Outline = Color(0xFFE2E8F0)
val Scrim = Color(0xFF000000)

// Gradients
val PrimaryGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF00E5DD), Color(0xFF00CFC8))
)

val WelcomeGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
)

val CardShadow = Color(0x0D000000)
