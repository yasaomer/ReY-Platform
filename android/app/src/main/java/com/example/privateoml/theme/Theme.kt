package com.example.privateoml.theme

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
  primary = Primary,
  secondary = Secondary,
  tertiary = Tertiary,
  background = BgBase,
  surface = BgSurface,
  surfaceVariant = BgCard,
  onPrimary = Color.Black,
  onSecondary = Color.Black,
  onTertiary = Color.Black,
  onBackground = TextPrimary,
  onSurface = TextPrimary,
  onSurfaceVariant = TextSecondary,
  error = ColorError
)

@Composable
fun PrivateOMLTheme(
  darkTheme: Boolean = true, // Force dark theme for premium styling
  dynamicColor: Boolean = false, // Force disable dynamic colors
  content: @Composable () -> Unit,
) {
  // Always use our DarkColorScheme to preserve premium dark mode aesthetics
  val colorScheme = DarkColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
