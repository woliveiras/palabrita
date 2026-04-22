package com.woliveiras.palabrita.feature.game

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.model.GameSession
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.PuzzleSource
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
  fun `initial status is CHOOSING_DIFFICULTY`() = runTest {
    val vm = createViewModel()
    assertThat(vm.state.value.gameStatus).isEqualTo(GameStatus.CHOOSING_DIFFICULTY)
  }

  @Test
  fun `initial attempts list is empty`() = runTest {
    val vm = createViewModel()
    assertThat(vm.state.value.attempts).isEmpty()
  }

  @Test
  fun `initial current input is empty`() = runTest {
    val vm = createViewModel()
    assertThat(vm.state.value.currentInput).isEmpty()
  }

  // --- Difficulty selection ---

  @Test
  fun `load difficulty options populates available difficulties`() = runTest {
    val vm = createViewModel(stats = PlayerStats(currentDifficulty = 1, maxUnlockedDifficulty = 2))
    vm.loadDifficultyOptions()
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.availableDifficulties).hasSize(5)
  }

  @Test
  fun `selecting unlocked difficulty updates chosen difficulty`() = runTest {
    val vm = createViewModel(stats = PlayerStats(currentDifficulty = 1, maxUnlockedDifficulty = 3))
    vm.loadDifficultyOptions()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(GameAction.SelectDifficulty(2))
    assertThat(vm.state.value.chosenDifficulty).isEqualTo(2)
  }

  @Test
  fun `selecting locked difficulty does not change chosen difficulty`() = runTest {
    val vm = createViewModel(stats = PlayerStats(currentDifficulty = 1, maxUnlockedDifficulty = 1))
    vm.loadDifficultyOptions()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(GameAction.SelectDifficulty(4))
    assertThat(vm.state.value.chosenDifficulty).isEqualTo(1)
  }

  // --- Game start ---

  @Test
  fun `starting game transitions to PLAYING with puzzle`() = runTest {
    val puzzle = createTestPuzzle()
    val vm = createViewModel(puzzle = puzzle)
    vm.loadDifficultyOptions()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(GameAction.StartGame)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.gameStatus).isEqualTo(GameStatus.PLAYING)
    assertThat(vm.state.value.puzzle).isEqualTo(puzzle)
  }

  @Test
  fun `starting game with no puzzle available emits NoPuzzlesLeft`() = runTest {
    val vm = createViewModel(puzzle = null)
    vm.loadDifficultyOptions()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.events.test {
      vm.onAction(GameAction.StartGame)
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
  fun `cannot reveal more than 5 hints`() = runTest {
    val vm = createPlayingViewModel()
    repeat(6) { vm.onAction(GameAction.RevealHint) }
    assertThat(vm.state.value.revealedHints).hasSize(5)
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
    hints: List<String> = listOf("Dica 1", "Dica 2", "Dica 3", "Dica 4", "Dica 5"),
  ) =
    Puzzle(
      id = 1,
      word = word,
      wordDisplay = word.uppercase(),
      language = "pt",
      difficulty = 1,
      category = "Animal",
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
    vm.loadDifficultyOptions()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(GameAction.StartGame)
    testDispatcher.scheduler.advanceUntilIdle()
    return vm
  }
}

private class FakePuzzleRepository(private val puzzle: Puzzle? = null) : PuzzleRepository {
  val markedPlayed = mutableListOf<Long>()

  override suspend fun getNextUnplayed(language: String, difficulty: Int): Puzzle? = puzzle

  override suspend fun countUnplayed(language: String, difficulty: Int): Int =
    if (puzzle != null) 1 else 0

  override suspend fun countAllUnplayed(language: String): Int = if (puzzle != null) 1 else 0

  override suspend fun getAllGeneratedWords(): Set<String> = emptySet()

  override suspend fun getRecentWords(limit: Int): List<String> = emptyList()

  override suspend fun savePuzzle(puzzle: Puzzle): Long = puzzle.id

  override suspend fun savePuzzles(puzzles: List<Puzzle>) {}

  override suspend fun markAsPlayed(puzzleId: Long) {
    markedPlayed.add(puzzleId)
  }

  override suspend fun deleteUnplayedAiPuzzles() {}

  override suspend fun markAllUnplayed() {}
}

private class FakeStatsRepository(private var stats: PlayerStats = PlayerStats()) :
  StatsRepository {
  override suspend fun getStats(): PlayerStats = stats

  override suspend fun updateAfterGame(
    won: Boolean,
    attempts: Int,
    difficulty: Int,
    hintsUsed: Int,
  ) {}

  override suspend fun checkAndPromoteDifficulty(): Int = stats.currentDifficulty

  override suspend fun updateLanguage(language: String) {}

  override suspend fun updateWordSizePreference(preference: String) {}

  override suspend fun resetProgress() {}

  override fun observeStats(): Flow<PlayerStats> = flowOf(stats)
}

private class FakeGameSessionRepository : GameSessionRepository {
  val sessions = mutableListOf<GameSession>()

  override suspend fun create(session: GameSession): Long {
    sessions.add(session)
    return session.id
  }

  override suspend fun update(session: GameSession) {
    sessions.removeAll { it.puzzleId == session.puzzleId }
    sessions.add(session)
  }

  override suspend fun getByPuzzleId(puzzleId: Long): GameSession? =
    sessions.find { it.puzzleId == puzzleId }

  override suspend fun hasActiveGame(): Boolean = sessions.any { it.completedAt == null }

  override suspend fun completeSession(
    puzzleId: Long,
    attempts: List<String>,
    completedAt: Long,
    hintsUsed: Int,
    won: Boolean,
  ) {
    val session = sessions.find { it.puzzleId == puzzleId } ?: return
    sessions.remove(session)
    sessions.add(session.copy(completedAt = completedAt, won = won))
  }

  override suspend fun deleteAll() {
    sessions.clear()
  }
}
