package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val UltronDarkColorScheme =
  darkColorScheme(
    primary = UltronCyan,
    secondary = UltronGreen,
    tertiary = UltronGold,
    background = UltronDarkBg,
    surface = UltronSurface,
    surfaceVariant = UltronSurfaceVariant,
    onPrimary = UltronDarkBg,
    onSecondary = UltronDarkBg,
    onBackground = UltronTextPrimary,
    onSurface = UltronTextPrimary,
    error = UltronErrorRed
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = UltronDarkColorScheme,
    typography = Typography,
    content = content
  )
}
