package com.woliveiras.palabrita.feature.game

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.PuzzleSource
import com.woliveiras.palabrita.core.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Initial state ---

  @Test
  fun `init auto-starts game and transitions to PLAYING`() = runTest {
    val puzzle = createTestPuzzle()
    val vm = createViewModel(puzzle = puzzle)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.gameStatus).isEqualTo(GameStatus.PLAYING)
    assertThat(vm.state.value.puzzle).isEqualTo(puzzle)
  }

  @Test
  fun `initial attempts list is empty`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.attempts).isEmpty()
  }

  @Test
  fun `initial current input is empty`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentInput).isEmpty()
  }

  // --- Game start ---

  @Test
  fun `no puzzle available emits NoPuzzlesLeft`() = runTest {
    val vm = createViewModel(puzzle = null)
    vm.events.test {
      testDispatcher.scheduler.advanceUntilIdle()
      assertThat(awaitItem()).isEqualTo(GameEvent.NoPuzzlesLeft)
    }
  }

  // --- Typing ---

  @Test
  fun `typing a letter appends to current input`() = runTest {
    val vm = createPlayingViewModel()
    vm.onAction(GameAction.TypeLetter('a'))
    assertThat(vm.state.value.currentInput).isEqualTo("a")
  }

  @Test
  fun `typing converts to lowercase`() = runTest {
    val vm = createPlayingViewModel()
    vm.onAction(GameAction.TypeLetter('A'))
    assertThat(vm.state.value.currentInput).isEqualTo("a")
  }

  @Test
  fun `typing beyond word length is ignored`() = runTest {
    val vm = createPlayingViewModel(word = "gatos") // 5 letters
    "gatos".forEach { vm.onAction(GameAction.TypeLetter(it)) }
    vm.onAction(GameAction.TypeLetter('x'))
    assertThat(vm.state.value.currentInput).isEqualTo("gatos")
  }

  @Test
  fun `deleting removes last letter`() = runTest {
    val vm = createPlayingViewModel()
    vm.onAction(GameAction.TypeLetter('a'))
    vm.onAction(GameAction.TypeLetter('b'))
    vm.onAction(GameAction.DeleteLetter)
    assertThat(vm.state.value.currentInput).isEqualTo("a")
  }

  @Test
  fun `deleting from empty input does nothing`() = runTest {
    val vm = createPlayingViewModel()
    vm.onAction(GameAction.DeleteLetter)
    assertThat(vm.state.value.currentInput).isEmpty()
  }

  // --- Submitting attempts ---

  @Test
  fun `submitting correct word results in WON`() = runTest {
    val vm = createPlayingViewModel(word = "gatos")
    "gatos".forEach { vm.onAction(GameAction.TypeLetter(it)) }
    vm.onAction(GameAction.SubmitAttempt)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.gameStatus).isEqualTo(GameStatus.WON)
  }

  @Test
  fun `submitting wrong word adds to attempts and stays PLAYING`() = runTest {
    val vm = createPlayingViewModel(word = "gatos")
    "puxar".forEach { vm.onAction(GameAction.TypeLetter(it)) }
    vm.onAction(GameAction.SubmitAttempt)
    assertThat(vm.state.value.gameStatus).isEqualTo(GameStatus.PLAYING)
    assertThat(vm.state.value.attempts).hasSize(1)
  }

  @Test
  fun `submitting clears current input`() = runTest {
    val vm = createPlayingViewModel(word = "gatos")
    "puxar".forEach { vm.onAction(GameAction.TypeLetter(it)) }
    vm.onAction(GameAction.SubmitAttempt)
    assertThat(vm.state.value.currentInput).isEmpty()
  }

  @Test
  fun `submitting with wrong length is ignored`() = runTest {
    val vm = createPlayingViewModel(word = "gatos")
    "gat".forEach { vm.onAction(GameAction.TypeLetter(it)) }
    vm.onAction(GameAction.SubmitAttempt)
    assertThat(vm.state.value.attempts).isEmpty()
    assertThat(vm.state.value.currentInput).isEqualTo("gat")
  }

  @Test
  fun `6th wrong attempt results in LOST`() = runTest {
    val vm = createPlayingViewModel(word = "gatos")
    repeat(6) {
      "puxar".forEach { c -> vm.onAction(GameAction.TypeLetter(c)) }
      vm.onAction(GameAction.SubmitAttempt)
    }
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.gameStatus).isEqualTo(GameStatus.LOST)
    assertThat(vm.state.value.attempts).hasSize(6)
  }

  @Test
  fun `attempt feedback is calculated correctly`() = runTest {
    val vm = createPlayingViewModel(word = "gatos")
    "galho".forEach { vm.onAction(GameAction.TypeLetter(it)) }
    vm.onAction(GameAction.SubmitAttempt)
    val feedback = vm.state.value.attempts[0].feedback
    assertThat(feedback[0].state).isEqualTo(LetterState.CORRECT) // g
    assertThat(feedback[1].state).isEqualTo(LetterState.CORRECT) // a
    assertThat(feedback[2].state).isEqualTo(LetterState.ABSENT) // l
    assertThat(feedback[3].state).isEqualTo(LetterState.ABSENT) // h
    assertThat(feedback[4].state).isEqualTo(LetterState.PRESENT) // o
  }

  @Test
  fun `keyboard updates after submitting attempt`() = runTest {
    val vm = createPlayingViewModel(word = "gatos")
    "galho".forEach { vm.onAction(GameAction.TypeLetter(it)) }
    vm.onAction(GameAction.SubmitAttempt)
    assertThat(vm.state.value.keyboardState['g']).isEqualTo(LetterState.CORRECT)
    assertThat(vm.state.value.keyboardState['a']).isEqualTo(LetterState.CORRECT)
    assertThat(vm.state.value.keyboardState['l']).isEqualTo(LetterState.ABSENT)
  }

  // --- Hints ---

  @Test
  fun `revealing hint adds first hint to revealed list`() = runTest {
    val vm = createPlayingViewModel()
    vm.onAction(GameAction.RevealHint)
    assertThat(vm.state.value.revealedHints).hasSize(1)
    assertThat(vm.state.value.revealedHints[0]).isEqualTo("Dica 1")
  }

  @Test
  fun `hints reveal progressively`() = runTest {
    val vm = createPlayingViewModel()
    vm.onAction(GameAction.RevealHint)
    vm.onAction(GameAction.RevealHint)
    vm.onAction(GameAction.RevealHint)
    assertThat(vm.state.value.revealedHints).hasSize(3)
    assertThat(vm.state.value.revealedHints).containsExactly("Dica 1", "Dica 2", "Dica 3").inOrder()
  }

  @Test
  fun `cannot reveal more hints than available`() = runTest {
    val vm = createPlayingViewModel()
    repeat(5) { vm.onAction(GameAction.RevealHint) }
    assertThat(vm.state.value.revealedHints).hasSize(3)
  }

  @Test
  fun `hints not available when game is not PLAYING`() = runTest {
    val vm = createViewModel()
    vm.onAction(GameAction.RevealHint)
    assertThat(vm.state.value.revealedHints).isEmpty()
  }

  // --- State flow ---

  @Test
  fun `state flow emits updates on action`() = runTest {
    val vm = createPlayingViewModel()
    vm.state.test {
      assertThat(awaitItem().gameStatus).isEqualTo(GameStatus.PLAYING)
      vm.onAction(GameAction.TypeLetter('a'))
      assertThat(awaitItem().currentInput).isEqualTo("a")
      cancelAndIgnoreRemainingEvents()
    }
  }

  // --- Win/Loss triggers stats update ---

  @Test
  fun `winning marks puzzle as played`() = runTest {
    val puzzleRepo = FakePuzzleRepository(createTestPuzzle())
    val vm = createPlayingViewModel(puzzleRepo = puzzleRepo, word = "gatos")
    "gatos".forEach { vm.onAction(GameAction.TypeLetter(it)) }
    vm.onAction(GameAction.SubmitAttempt)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(puzzleRepo.markedPlayed).contains(1L)
  }

  @Test
  fun `losing marks puzzle as played`() = runTest {
    val puzzleRepo = FakePuzzleRepository(createTestPuzzle())
    val vm = createPlayingViewModel(puzzleRepo = puzzleRepo, word = "gatos")
    repeat(6) {
      "puxar".forEach { c -> vm.onAction(GameAction.TypeLetter(c)) }
      vm.onAction(GameAction.SubmitAttempt)
    }
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(puzzleRepo.markedPlayed).contains(1L)
  }

  // --- Helpers ---

  private fun createTestPuzzle(
    word: String = "gatos",
    hints: List<String> = listOf("Dica 1", "Dica 2", "Dica 3"),
  ) =
    Puzzle(
      id = 1,
      word = word,
      wordDisplay = word.uppercase(),
      language = "pt",
      difficulty = 1,
      category = "",
      hints = hints,
      source = PuzzleSource.AI,
      generatedAt = System.currentTimeMillis(),
    )

  private fun createViewModel(
    stats: PlayerStats = PlayerStats(),
    puzzle: Puzzle? = createTestPuzzle(),
    puzzleRepo: FakePuzzleRepository = FakePuzzleRepository(puzzle),
  ): GameViewModel =
    GameViewModel(
      puzzleRepository = puzzleRepo,
      statsRepository = FakeStatsRepository(stats),
      gameSessionRepository = FakeGameSessionRepository(),
    )

  private fun createPlayingViewModel(
    word: String = "gatos",
    puzzleRepo: FakePuzzleRepository = FakePuzzleRepository(createTestPuzzle(word = word)),
  ): GameViewModel {
    val vm =
      GameViewModel(
        puzzleRepository = puzzleRepo,
        statsRepository = FakeStatsRepository(),
        gameSessionRepository = FakeGameSessionRepository(),
      )
    testDispatcher.scheduler.advanceUntilIdle()
    return vm
  }
}
