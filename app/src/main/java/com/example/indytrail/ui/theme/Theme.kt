package com.example.indytrail.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary      = AetherBlue,
    secondary    = SunGold,
    tertiary     = RuneCyan,
    background   = Obsidian,
    surface      = SurfaceDark,
    onPrimary    = TextOnTone,
    onSecondary  = TextOnTone,
    onTertiary   = TextOnTone,
    onBackground = TextOnDark,
    onSurface    = TextOnDark,
)

@Composable
fun IndyTrailTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = Typography, // kommt aus Type.kt
        content = content
    )
}