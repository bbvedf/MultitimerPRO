package com.android.multitimerpro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonBlue,
    secondary = NeonGreen,
    tertiary = NeonPurple,
    background = DeepBlack,
    surface = DeepBlack,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant
)

@Composable
fun MultiTimerProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}