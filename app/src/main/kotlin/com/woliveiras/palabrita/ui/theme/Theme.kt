package com.woliveiras.palabrita.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.woliveiras.palabrita.core.common.GameColors
import com.woliveiras.palabrita.core.common.LocalGameColors
import com.woliveiras.palabrita.core.common.PalabritaColors

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondaryContainer = PalabritaColors.ContainerBlue,
    onSecondaryContainer = PalabritaColors.OnContainerBlue,
    background = PalabritaColors.BackgroundLight,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = LightError,
    onError = LightOnError,
    outline = LightOutline,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondaryContainer = PalabritaColors.ContainerBlueDark,
    onSecondaryContainer = PalabritaColors.OnContainerBlueDark,
    background = PalabritaColors.BackgroundDark,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = DarkError,
    onError = DarkOnError,
    outline = DarkOutline,
  )

@Composable
fun PalabritaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val gameColors =
    if (darkTheme) {
      GameColors(unused = PalabritaColors.TileUnusedDark)
    } else {
      GameColors()
    }

  CompositionLocalProvider(LocalGameColors provides gameColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = PalabritaTypography,
      shapes = PalabritaShapes,
      content = content,
    )
  }
}
