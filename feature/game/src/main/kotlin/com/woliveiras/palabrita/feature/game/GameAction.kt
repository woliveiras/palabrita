package com.woliveiras.palabrita.feature.game

import com.woliveiras.palabrita.core.model.ModelId

sealed class GameAction {
  data class SelectDifficulty(val level: Int) : GameAction()
  data object StartGame : GameAction()
  data class TypeLetter(val letter: Char) : GameAction()
  data object DeleteLetter : GameAction()
  data object SubmitAttempt : GameAction()
  data object RevealHint : GameAction()
  data object ShareResult : GameAction()
  data object NavigateToChat : GameAction()
  data object NavigateToStats : GameAction()
  data object LoadNextPuzzle : GameAction()
  data object BackPressed : GameAction()
  data object ConfirmAbandon : GameAction()
  data object DismissAbandonDialog : GameAction()
}
