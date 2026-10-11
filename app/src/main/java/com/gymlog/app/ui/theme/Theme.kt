package com.gymlog.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF3B82F6)
val Gold = Color(0xFFFFC233)
val Success = Color(0xFF22C55E)
val WarmupColor = Color(0xFFF59E0B)
val DropColor = Color(0xFFA78BFA)
val FailureColor = Color(0xFFEF4444)

private val scheme = darkColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF0E0E11),
    secondaryContainer = Color(0xFF26324A),
    onSecondaryContainer = Color(0xFFDBEAFE),
    background = Color(0xFF0E0E11),
    onBackground = Color(0xFFF1F1F3),
    surface = Color(0xFF0E0E11),
    onSurface = Color(0xFFF1F1F3),
    surfaceVariant = Color(0xFF26262C),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainerLowest = Color(0xFF0B0B0D),
    surfaceContainerLow = Color(0xFF141417),
    surfaceContainer = Color(0xFF18181C),
    surfaceContainerHigh = Color(0xFF202026),
    surfaceContainerHighest = Color(0xFF2A2A31),
    outline = Color(0xFF3F3F46),
    outlineVariant = Color(0xFF2E2E35),
    error = Color(0xFFEF4444),
    onError = Color.White,
)

@Composable
fun GymTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
