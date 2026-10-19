// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.arntcli.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ArnTCLIDarkColorScheme = darkColorScheme(
    primary = ElectricSlateTokens.PrimaryAccent,
    onPrimary = Color.White,
    primaryContainer = ElectricSlateTokens.PrimaryAccent,
    onPrimaryContainer = Color.White,
    inversePrimary = ElectricSlateTokens.PrimaryAccent,

    secondary = ElectricSlateTokens.TrackInactive,
    onSecondary = ElectricSlateTokens.TextPrimary,
    secondaryContainer = ElectricSlateTokens.SurfaceElevated,
    onSecondaryContainer = ElectricSlateTokens.TextPrimary,

    tertiary = ElectricSlateTokens.PrimaryAccent,
    onTertiary = ElectricSlateTokens.SurfacePanel,
    tertiaryContainer = ElectricSlateTokens.BorderSubtle,
    onTertiaryContainer = ElectricSlateTokens.TextPrimary,

    background = ElectricSlateTokens.BgCanvas,
    onBackground = ElectricSlateTokens.TextPrimary,

    surface = ElectricSlateTokens.BgCanvas,
    onSurface = ElectricSlateTokens.TextPrimary,

    surfaceContainerLowest = ElectricSlateTokens.BgCanvas,
    surfaceContainerLow = ElectricSlateTokens.SurfacePanel,
    surfaceContainer = ElectricSlateTokens.SurfacePanel,
    surfaceContainerHigh = ElectricSlateTokens.SurfaceElevated,
    surfaceContainerHighest = ElectricSlateTokens.SurfaceElevated,

    surfaceVariant = ElectricSlateTokens.SurfacePanel,
    onSurfaceVariant = ElectricSlateTokens.TextMuted,

    outline = ElectricSlateTokens.BorderSubtle,
    outlineVariant = ElectricSlateTokens.BorderSubtle,

    error = ElectricSlateTokens.Destructive,
    onError = Color.White
)

@Composable
fun ArnTCLITheme(
    darkTheme: Boolean = true, // Dark dominant per Android.md
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ArnTCLIDarkColorScheme,
        typography = ArnTCLITypography,
        content = content
    )
}
