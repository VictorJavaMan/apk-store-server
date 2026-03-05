package com.apkstore.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3DDC84),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F5CD),
    onPrimaryContainer = Color(0xFF002110),
    secondary = Color(0xFF4285F4),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E3FF),
    onSecondaryContainer = Color(0xFF001A41),
    tertiary = Color(0xFFFF5722),
    onTertiary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3DDC84),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF00522F),
    onPrimaryContainer = Color(0xFFB8F5CD),
    secondary = Color(0xFFADC6FF),
    onSecondary = Color(0xFF002E69),
    secondaryContainer = Color(0xFF004494),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = Color(0xFFFFB59D),
    onTertiary = Color(0xFF5F1500),
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

@Composable
fun ApkStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
