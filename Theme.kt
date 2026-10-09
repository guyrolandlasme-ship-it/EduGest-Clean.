package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EduIndigoLight,
    onPrimary = Color(0xFF0B1020),
    primaryContainer = EduIndigoDark,
    onPrimaryContainer = Color.White,
    secondary = EduSky,
    onSecondary = Color.White,
    tertiary = EduGold,
    onTertiary = Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextMutedDark,
    outline = OutlineDark,
    outlineVariant = Color(0xFF1E2842),
    error = EduRedAlert,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = EduIndigo,
    onPrimary = Color.White,
    primaryContainer = EduIndigoSurface,
    onPrimaryContainer = EduIndigoDark,
    secondary = EduSky,
    onSecondary = Color.White,
    secondaryContainer = EduSkyLight,
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = EduGold,
    onTertiary = Color.White,
    tertiaryContainer = EduGoldLight,
    onTertiaryContainer = Color(0xFF92400E),
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextMutedLight,
    outline = OutlineLight,
    outlineVariant = Color(0xFFE2E8F0),
    error = EduRedAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep signature EduGest scholastic brand colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
