package com.woliveiras.palabrita.core.common

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GameColors(
  val correct: Color = PalabritaColors.TileCorrect,
  val present: Color = PalabritaColors.TilePresent,
  val absent: Color = PalabritaColors.TileAbsent,
  val unused: Color = PalabritaColors.TileUnused,
  val onFeedback: Color = PalabritaColors.OnTile,
)

val LocalGameColors = staticCompositionLocalOf { GameColors() }
