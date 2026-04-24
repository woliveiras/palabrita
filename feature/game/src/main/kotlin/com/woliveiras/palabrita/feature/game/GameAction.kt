package com.woliveiras.palabrita.feature.game

sealed class GameAction {
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

  data object ClearShake : GameAction()
}
