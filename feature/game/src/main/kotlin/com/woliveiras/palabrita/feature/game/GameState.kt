package com.woliveiras.palabrita.feature.game

import androidx.annotation.StringRes

data class GameState(
  val puzzle: com.woliveiras.palabrita.core.model.Puzzle? = null,
  val attempts: List<Attempt> = emptyList(),
  val currentInput: String = "",
  val revealedHints: List<String> = emptyList(),
  val keyboardState: Map<Char, LetterState> = emptyMap(),
  val gameStatus: GameStatus = GameStatus.LOADING,
  val isLoading: Boolean = false,
  @StringRes val errorRes: Int? = null,
  val showAbandonDialog: Boolean = false,
  val showShake: Boolean = false,
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
  PLAYING,
  WON,
  LOST,
  LOADING,
}
