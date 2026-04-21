package com.woliveiras.palabrita.core.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GameColors(
  val correct: Color = Color(0xFF4ECDC4),
  val present: Color = Color(0xFFFFB347),
  val absent: Color = Color(0xFFFF6B6B),
  val unused: Color = Color(0xFF787C7E),
  val onFeedback: Color = Color(0xFF1A1A2E),
)

val LocalGameColors = staticCompositionLocalOf { GameColors() }
