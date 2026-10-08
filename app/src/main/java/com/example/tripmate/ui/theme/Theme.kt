package com.example.tripmate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Exact light color scheme mapping based on DESIGN.md
private val TripMateLightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,

    secondary = SecondaryColor,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary = TertiaryColor,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    background = BackgroundColor,
    onBackground = OnBackground,

    surface = SurfaceColor,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = SurfaceTint,
    inverseSurface = InverseSurface,
    inverseOnSurface = InverseOnSurface,

    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,

    error = ErrorColor,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    outline = Outline,
    outlineVariant = OutlineVariant,
)

// Modern sleek dark color scheme based on slate-900 & coral orange
private val TripMateDarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    inversePrimary = InversePrimary,

    secondary = SecondaryColor,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    tertiary = TertiaryColor,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    background = androidx.compose.ui.graphics.Color(0xFF0F172A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF8FAFC),

    surface = androidx.compose.ui.graphics.Color(0xFF1E293B),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF334155),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF94A3B8),
    surfaceTint = SurfaceTint,
    inverseSurface = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
    inverseOnSurface = androidx.compose.ui.graphics.Color(0xFF0F172A),

    surfaceDim = androidx.compose.ui.graphics.Color(0xFF0F172A),
    surfaceBright = androidx.compose.ui.graphics.Color(0xFF1E293B),
    surfaceContainerLowest = androidx.compose.ui.graphics.Color(0xFF0B1120),
    surfaceContainerLow = androidx.compose.ui.graphics.Color(0xFF0F172A),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF1E293B),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF334155),
    surfaceContainerHighest = androidx.compose.ui.graphics.Color(0xFF475569),

    error = ErrorColor,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    outline = androidx.compose.ui.graphics.Color(0xFF334155),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF1E293B),
)

@Composable
fun TripMateTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TripMateDarkColorScheme else TripMateLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TripMateTypography,
        shapes = TripMateShapes,
        content = content
    )
}

@Composable
fun TripPilotTheme(
    content: @Composable () -> Unit
) {
    TripMateTheme(content = content)
}