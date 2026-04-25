package com.woliveiras.palabrita.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.model.GameRules
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class GameEvent {
  data object NavigateToHome : GameEvent()

  data object NoPuzzlesLeft : GameEvent()
}

@HiltViewModel
class GameViewModel
@Inject
constructor(
  private val puzzleRepository: PuzzleRepository,
  private val statsRepository: StatsRepository,
  private val gameSessionRepository: GameSessionRepository,
) : ViewModel() {

  private val _state = MutableStateFlow(GameState())
  val state: StateFlow<GameState> = _state.asStateFlow()

  private val _events = MutableSharedFlow<GameEvent>()
  val events = _events.asSharedFlow()

  init {
    restoreOrLoadNext()
  }

  fun onAction(action: GameAction) {
    when (action) {
      is GameAction.TypeLetter -> typeLetter(action.letter)
      is GameAction.DeleteLetter -> deleteLetter()
      is GameAction.SubmitAttempt -> submitAttempt()
      is GameAction.RevealHint -> revealHint()
      is GameAction.ShareResult -> {
        /* handled by UI */
      }
      is GameAction.NavigateToChat -> {
        /* handled by UI */
      }
      is GameAction.NavigateToStats -> {
        /* handled by UI */
      }
      is GameAction.LoadNextPuzzle -> loadNextGame()
      is GameAction.BackPressed -> handleBackPressed()
      is GameAction.ConfirmAbandon -> confirmAbandon()
      is GameAction.DismissAbandonDialog -> _state.update { it.copy(showAbandonDialog = false) }
      is GameAction.ClearShake -> _state.update { it.copy(showShake = false) }
    }
  }

  private fun restoreOrLoadNext() {
    viewModelScope.launch {
      _state.update { it.copy(gameStatus = GameStatus.LOADING) }
      val session = gameSessionRepository.getActiveSession()
      if (session != null) {
        val puzzle = puzzleRepository.getById(session.puzzleId)
        if (puzzle != null) {
          val attempts =
            session.attempts.map { word ->
              Attempt(word = word, feedback = GameLogic.calculateLetterFeedback(word, puzzle.word))
            }
          val keyboardState =
            attempts.fold(emptyMap<Char, LetterState>()) { acc, attempt ->
              GameLogic.updateKeyboardState(acc, attempt.feedback)
            }
          _state.update {
            it.copy(
              puzzle = puzzle,
              gameStatus = GameStatus.PLAYING,
              attempts = attempts,
              currentInput = "",
              revealedHints = puzzle.hints.take(session.hintsUsed),
              keyboardState = keyboardState,
              errorRes = null,
            )
          }
          return@launch
        }
      }
      loadNextGameInternal()
    }
  }

  fun loadNextGame() {
    viewModelScope.launch {
      _state.update { it.copy(gameStatus = GameStatus.LOADING) }
      loadNextGameInternal()
    }
  }

  private suspend fun loadNextGameInternal() {
    val stats = statsRepository.getStats()
    val puzzle = puzzleRepository.getNextUnplayed(stats.preferredLanguage)
    if (puzzle != null) {
      _state.update {
        it.copy(
          puzzle = puzzle,
          gameStatus = GameStatus.PLAYING,
          attempts = emptyList(),
          currentInput = "",
          revealedHints = emptyList(),
          keyboardState = emptyMap(),
          errorRes = null,
        )
      }
      gameSessionRepository.create(
        com.woliveiras.palabrita.core.model.GameSession(
          puzzleId = puzzle.id,
          startedAt = System.currentTimeMillis(),
        )
      )
    } else {
      _events.emit(GameEvent.NoPuzzlesLeft)
    }
  }

  private fun typeLetter(letter: Char) {
    val puzzle = _state.value.puzzle ?: return
    if (_state.value.gameStatus != GameStatus.PLAYING) return
    val maxLen = puzzle.word.length
    if (_state.value.currentInput.length >= maxLen) return
    _state.update { it.copy(currentInput = it.currentInput + letter.lowercaseChar()) }
  }

  private fun deleteLetter() {
    if (_state.value.gameStatus != GameStatus.PLAYING) return
    if (_state.value.currentInput.isEmpty()) return
    _state.update { it.copy(currentInput = it.currentInput.dropLast(1)) }
  }

  private fun submitAttempt() {
    val current = _state.value
    val puzzle = current.puzzle ?: return
    if (current.gameStatus != GameStatus.PLAYING) return
    if (current.currentInput.length != puzzle.word.length) {
      _state.update { it.copy(showShake = true) }
      return
    }
    if (!current.currentInput.all { it in 'a'..'z' }) {
      _state.update { it.copy(showShake = true) }
      return
    }

    val feedback = GameLogic.calculateLetterFeedback(current.currentInput, puzzle.word)
    val attempt = Attempt(word = current.currentInput, feedback = feedback)
    val newAttempts = current.attempts + attempt
    val newKeyboard = GameLogic.updateKeyboardState(current.keyboardState, feedback)
    val won = feedback.all { it.state == LetterState.CORRECT }
    val lost = !won && newAttempts.size >= GameRules.MAX_ATTEMPTS

    val newStatus =
      when {
        won -> GameStatus.WON
        lost -> GameStatus.LOST
        else -> GameStatus.PLAYING
      }

    _state.update {
      it.copy(
        attempts = newAttempts,
        currentInput = "",
        keyboardState = newKeyboard,
        gameStatus = newStatus,
      )
    }

    if (won || lost) {
      viewModelScope.launch {
        statsRepository.updateAfterGame(
          won = won,
          attempts = newAttempts.size,
          hintsUsed = current.revealedHints.size,
        )
        puzzleRepository.markAsPlayed(puzzle.id)
        gameSessionRepository.completeSession(
          puzzleId = puzzle.id,
          attempts = newAttempts.map { it.word },
          completedAt = System.currentTimeMillis(),
          hintsUsed = current.revealedHints.size,
          won = won,
        )
      }
    } else {
      viewModelScope.launch {
        val session = gameSessionRepository.getByPuzzleId(puzzle.id)
        if (session != null) {
          gameSessionRepository.update(
            session.copy(
              attempts = newAttempts.map { it.word },
              hintsUsed = current.revealedHints.size,
            )
          )
        }
      }
    }
  }

  private fun revealHint() {
    val puzzle = _state.value.puzzle ?: return
    if (_state.value.gameStatus != GameStatus.PLAYING) return
    val revealed = _state.value.revealedHints.size
    if (revealed >= puzzle.hints.size) return
    _state.update { it.copy(revealedHints = it.revealedHints + puzzle.hints[revealed]) }
  }

  private fun handleBackPressed() {
    val status = _state.value.gameStatus
    if (status == GameStatus.PLAYING) {
      _state.update { it.copy(showAbandonDialog = true) }
    } else {
      viewModelScope.launch { _events.emit(GameEvent.NavigateToHome) }
    }
  }

  private fun confirmAbandon() {
    _state.update { it.copy(showAbandonDialog = false) }
    viewModelScope.launch { _events.emit(GameEvent.NavigateToHome) }
  }
}
