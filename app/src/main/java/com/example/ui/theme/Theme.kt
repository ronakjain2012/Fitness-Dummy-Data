package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FitnessColorScheme = darkColorScheme(
    primary = FitnessPrimary,
    onPrimary = FitnessOnBackground,
    primaryContainer = FitnessPrimaryContainer,
    secondary = FitnessSecondary,
    onSecondary = FitnessOnBackground,
    secondaryContainer = FitnessSecondaryContainer,
    tertiary = FitnessHeart,
    background = FitnessBackground,
    onBackground = FitnessOnBackground,
    surface = FitnessSurface,
    onSurface = FitnessOnSurface,
    surfaceVariant = FitnessSurfaceVariant,
    onSurfaceVariant = FitnessOnSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FitnessColorScheme,
        typography = Typography,
        content = content
    )
}
