package com.courseadvisor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = SurfaceWhite,
    secondary = SageSecondary,
    onSecondary = TextDark,
    background = BackgroundMint,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = SageLight,
    onSurfaceVariant = TextMuted,
    outline = BorderSubtle
)

@Composable
fun CourseAdvisorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
