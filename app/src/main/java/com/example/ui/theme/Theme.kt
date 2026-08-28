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

private val DarkColorScheme =
  darkColorScheme(
    primary = EditorialPrimaryDark,
    onPrimary = EditorialOnPrimaryDark,
    primaryContainer = EditorialPrimaryContainerDark,
    onPrimaryContainer = EditorialOnPrimaryContainerDark,
    secondary = EditorialSecondaryDark,
    onSecondary = Color(0xFF141914),
    secondaryContainer = EditorialSecondaryContainerDark,
    onSecondaryContainer = EditorialOnSecondaryContainerDark,
    tertiary = Color(0xFFD4C3A3),
    onTertiary = Color(0xFF382E1C),
    tertiaryContainer = Color(0xFF4F4430),
    onTertiaryContainer = Color(0xFFF1E4CB),
    background = EditorialBackgroundDark,
    onBackground = EditorialOnBackgroundDark,
    surface = EditorialSurfaceDark,
    onSurface = EditorialOnSurfaceDark,
    surfaceVariant = EditorialSurfaceVariantDark,
    onSurfaceVariant = EditorialOnSurfaceVariantDark,
    outline = EditorialOutlineDark,
    outlineVariant = EditorialOutlineVariantDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EditorialPrimary,
    onPrimary = EditorialOnPrimary,
    primaryContainer = EditorialPrimaryContainer,
    onPrimaryContainer = EditorialOnPrimaryContainer,
    secondary = EditorialSecondary,
    onSecondary = Color.White,
    secondaryContainer = EditorialSecondaryContainer,
    onSecondaryContainer = EditorialOnSecondaryContainer,
    tertiary = EditorialTertiary,
    onTertiary = Color.White,
    tertiaryContainer = EditorialTertiaryContainer,
    onTertiaryContainer = EditorialOnTertiaryContainer,
    background = EditorialBackground,
    onBackground = EditorialOnBackground,
    surface = EditorialSurface,
    onSurface = EditorialOnSurface,
    surfaceVariant = EditorialSurfaceVariant,
    onSurfaceVariant = EditorialOnSurfaceVariant,
    outline = EditorialOutline,
    outlineVariant = EditorialOutlineVariant,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Keep consistent custom Editorial Aesthetic theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

