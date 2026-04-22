package com.woliveiras.palabrita.feature.game

import androidx.annotation.StringRes

data class GameState(
  val puzzle: com.woliveiras.palabrita.core.model.Puzzle? = null,
  val chosenDifficulty: Int = 1,
  val availableDifficulties: List<DifficultyOption> = emptyList(),
  val attempts: List<Attempt> = emptyList(),
  val currentInput: String = "",
  val revealedHints: List<String> = emptyList(),
  val keyboardState: Map<Char, LetterState> = emptyMap(),
  val gameStatus: GameStatus = GameStatus.CHOOSING_DIFFICULTY,
  val isLoading: Boolean = false,
  @StringRes val errorRes: Int? = null,
  val showAbandonDialog: Boolean = false,
  val xpGained: Int = 0,
)

data class DifficultyOption(
  val level: Int,
  @StringRes val labelRes: Int,
  val baseXp: Int,
  val isUnlocked: Boolean,
  val isSelectable: Boolean,
  val isRecommended: Boolean,
)

data class Attempt(val word: String, val feedback: List<LetterFeedback>)

data class LetterFeedback(val letter: Char, val state: LetterState)

enum class LetterState {
  CORRECT,
  PRESENT,
  ABSENT,
  UNUSED,
}

enum class GameStatus {
  CHOOSING_DIFFICULTY,
  PLAYING,
  WON,
  LOST,
  LOADING,
}
