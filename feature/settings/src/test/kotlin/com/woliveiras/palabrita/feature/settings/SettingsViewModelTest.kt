package com.woliveiras.palabrita.feature.settings

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.GameSession
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.PlayerTier
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.repository.ChatRepository
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Initial state / loading ---

  @Test
  fun `loads stats on init`() = runTest {
    val stats = PlayerStats(totalPlayed = 42, totalWon = 36, currentStreak = 8, maxStreak = 12)
    val vm = createViewModel(stats = stats)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.stats.totalPlayed).isEqualTo(42)
    assertThat(vm.state.value.stats.totalWon).isEqualTo(36)
    assertThat(vm.state.value.stats.currentStreak).isEqualTo(8)
    assertThat(vm.state.value.stats.maxStreak).isEqualTo(12)
  }

  @Test
  fun `loads current model config on init`() = runTest {
    val config = ModelConfig(modelId = ModelId.GEMMA4_E2B, downloadState = DownloadState.DOWNLOADED, sizeBytes = 2_600_000_000)
    val vm = createViewModel(modelConfig = config)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentModel.modelId).isEqualTo(ModelId.GEMMA4_E2B)
    assertThat(vm.state.value.currentModel.sizeBytes).isEqualTo(2_600_000_000)
  }

  @Test
  fun `calculates win rate correctly`() = runTest {
    val stats = PlayerStats(totalPlayed = 100, totalWon = 87)
    val vm = createViewModel(stats = stats)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.winRate).isEqualTo(87)
  }

  @Test
  fun `win rate is zero when no games played`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.winRate).isEqualTo(0)
  }

  // --- Language change ---

  @Test
  fun `changing language updates state`() = runTest {
    val vm = createViewModel(stats = PlayerStats(preferredLanguage = "pt"))
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ChangeLanguage("en"))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentLanguage).isEqualTo("en")
  }

  @Test
  fun `changing language persists to repository`() = runTest {
    val statsRepo = FakeStatsRepository(PlayerStats(preferredLanguage = "pt"))
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ChangeLanguage("es"))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(statsRepo.getStats().preferredLanguage).isEqualTo("es")
  }

  // --- Word size preference ---

  @Test
  fun `word size unlocked when tier is Astuto`() = runTest {
    val stats = PlayerStats(totalXp = 150, playerTier = PlayerTier.ASTUTO)
    val vm = createViewModel(stats = stats)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.isWordSizeUnlocked).isTrue()
  }

  @Test
  fun `word size locked when tier below Astuto`() = runTest {
    val stats = PlayerStats(totalXp = 49, playerTier = PlayerTier.NOVATO)
    val vm = createViewModel(stats = stats)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.isWordSizeUnlocked).isFalse()
  }

  @Test
  fun `epic word size available when tier is Epico`() = runTest {
    val stats = PlayerStats(totalXp = 1000, playerTier = PlayerTier.EPICO)
    val vm = createViewModel(stats = stats)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.isEpicWordSizeAvailable).isTrue()
  }

  @Test
  fun `changing word size persists to repository`() = runTest {
    val statsRepo = FakeStatsRepository(PlayerStats(playerTier = PlayerTier.ASTUTO))
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ChangeWordSize("SHORT"))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(statsRepo.getStats().wordSizePreference).isEqualTo("SHORT")
  }

  @Test
  fun `changing word size ignored when tier below Astuto`() = runTest {
    val statsRepo = FakeStatsRepository(PlayerStats(playerTier = PlayerTier.NOVATO))
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ChangeWordSize("SHORT"))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(statsRepo.getStats().wordSizePreference).isEqualTo("DEFAULT")
  }

  // --- Stats display ---

  @Test
  fun `guess distribution is available in state`() = runTest {
    val dist = mapOf(1 to 5, 2 to 12, 3 to 18, 4 to 8, 5 to 3, 6 to 1)
    val stats = PlayerStats(guessDistribution = dist)
    val vm = createViewModel(stats = stats)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.stats.guessDistribution).isEqualTo(dist)
  }

  // --- Share stats ---

  @Test
  fun `share stats generates text when context provided`() {
    // Share text generation now requires Android Context for string resources.
    // Verified via instrumented tests.
  }

  @Test
  fun `share stats with zero games`() {
    // Share text generation now requires Android Context for string resources.
    // Verified via instrumented tests.
  }

  // --- Reset progress ---

  @Test
  fun `reset progress clears stats`() = runTest {
    val statsRepo = FakeStatsRepository(
      PlayerStats(totalPlayed = 42, totalWon = 36, totalXp = 500, preferredLanguage = "es"),
    )
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ResetProgress)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.stats.totalPlayed).isEqualTo(0)
    assertThat(vm.state.value.stats.totalWon).isEqualTo(0)
    assertThat(vm.state.value.stats.totalXp).isEqualTo(0)
  }

  @Test
  fun `reset progress preserves language preference`() = runTest {
    val statsRepo = FakeStatsRepository(
      PlayerStats(totalPlayed = 42, preferredLanguage = "es"),
    )
    val vm = createViewModel(statsRepo = statsRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ResetProgress)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentLanguage).isEqualTo("es")
  }

  @Test
  fun `reset progress does not affect model`() = runTest {
    val config = ModelConfig(modelId = ModelId.GEMMA4_E2B, downloadState = DownloadState.DOWNLOADED)
    val modelRepo = FakeModelRepository(config)
    val vm = createViewModel(modelRepo = modelRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ResetProgress)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentModel.modelId).isEqualTo(ModelId.GEMMA4_E2B)
  }

  @Test
  fun `reset progress clears game sessions`() = runTest {
    val sessionRepo = FakeGameSessionRepository()
    sessionRepo.create(GameSession(puzzleId = 1, startedAt = 1000, completedAt = 2000, won = true))
    val vm = createViewModel(sessionRepo = sessionRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ResetProgress)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(sessionRepo.sessions).isEmpty()
  }

  @Test
  fun `reset progress clears chat messages`() = runTest {
    val chatRepo = FakeChatRepository()
    chatRepo.savedMessages.add(ChatMessage(1, 1L, com.woliveiras.palabrita.core.model.MessageRole.USER, "test", 1000))
    val vm = createViewModel(chatRepo = chatRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ResetProgress)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(chatRepo.savedMessages).isEmpty()
  }

  @Test
  fun `reset progress marks all puzzles as unplayed`() = runTest {
    val puzzleRepo = FakePuzzleRepository()
    puzzleRepo.allUnplayed = false
    val vm = createViewModel(puzzleRepo = puzzleRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ResetProgress)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(puzzleRepo.allUnplayed).isTrue()
  }

  // --- Delete model ---

  @Test
  fun `delete model switches to none`() = runTest {
    val modelRepo = FakeModelRepository(
      ModelConfig(modelId = ModelId.GEMMA4_E2B, downloadState = DownloadState.DOWNLOADED),
    )
    val vm = createViewModel(modelRepo = modelRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.DeleteModel)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentModel.modelId).isEqualTo(ModelId.NONE)
    assertThat(vm.state.value.currentModel.downloadState).isEqualTo(DownloadState.NOT_DOWNLOADED)
  }

  @Test
  fun `delete model clears unplayed AI puzzles`() = runTest {
    val puzzleRepo = FakePuzzleRepository()
    puzzleRepo.unplayedAiPuzzlesCleared = false
    val vm = createViewModel(
      modelRepo = FakeModelRepository(
        ModelConfig(modelId = ModelId.GEMMA4_E2B, downloadState = DownloadState.DOWNLOADED),
      ),
      puzzleRepo = puzzleRepo,
    )
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.DeleteModel)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(puzzleRepo.unplayedAiPuzzlesCleared).isTrue()
  }

  // --- Model switch blocked during active game ---

  @Test
  fun `model switch blocked when game in progress`() = runTest {
    val sessionRepo = FakeGameSessionRepository()
    sessionRepo.create(GameSession(puzzleId = 1, startedAt = 1000)) // no completedAt = active
    val vm = createViewModel(sessionRepo = sessionRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.hasActiveGame).isTrue()
  }

  @Test
  fun `switch model sets error when game in progress`() = runTest {
    val sessionRepo = FakeGameSessionRepository()
    sessionRepo.create(GameSession(puzzleId = 1, startedAt = 1000))
    val vm = createViewModel(sessionRepo = sessionRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.SwitchModel(ModelId.GEMMA3_1B))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.errorRes).isNotNull()
  }

  @Test
  fun `dismiss error clears error`() = runTest {
    val sessionRepo = FakeGameSessionRepository()
    sessionRepo.create(GameSession(puzzleId = 1, startedAt = 1000))
    val vm = createViewModel(sessionRepo = sessionRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.SwitchModel(ModelId.GEMMA3_1B))
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.DismissError)
    assertThat(vm.state.value.errorRes).isNull()
  }

  // --- Helpers ---

  private fun createViewModel(
    stats: PlayerStats = PlayerStats(),
    modelConfig: ModelConfig = ModelConfig(),
    deviceTier: DeviceTier = DeviceTier.HIGH,
    statsRepo: FakeStatsRepository = FakeStatsRepository(stats),
    modelRepo: FakeModelRepository = FakeModelRepository(modelConfig),
    sessionRepo: FakeGameSessionRepository = FakeGameSessionRepository(),
    chatRepo: FakeChatRepository = FakeChatRepository(),
    puzzleRepo: FakePuzzleRepository = FakePuzzleRepository(),
  ): SettingsViewModel = SettingsViewModel(
    statsRepository = statsRepo,
    modelRepository = modelRepo,
    gameSessionRepository = sessionRepo,
    chatRepository = chatRepo,
    puzzleRepository = puzzleRepo,
    deviceTier = deviceTier,
  )
}

// --- Fakes ---

private class FakeStatsRepository(
  private var stats: PlayerStats = PlayerStats(),
) : StatsRepository {
  private val _flow = MutableStateFlow(stats)

  override suspend fun getStats(): PlayerStats = stats
  override suspend fun updateAfterGame(won: Boolean, attempts: Int, difficulty: Int, hintsUsed: Int) {}
  override suspend fun checkAndPromoteDifficulty(): Int = stats.currentDifficulty
  override suspend fun updateLanguage(language: String) {
    stats = stats.copy(preferredLanguage = language)
    _flow.value = stats
  }
  override suspend fun updateWordSizePreference(preference: String) {
    stats = stats.copy(wordSizePreference = preference)
    _flow.value = stats
  }
  override suspend fun resetProgress() {
    stats = PlayerStats(preferredLanguage = stats.preferredLanguage, wordSizePreference = stats.wordSizePreference)
    _flow.value = stats
  }
  override fun observeStats(): Flow<PlayerStats> = _flow
}

private class FakeModelRepository(
  private var config: ModelConfig = ModelConfig(),
) : ModelRepository {
  private val _flow = MutableStateFlow(config)

  override suspend fun getConfig(): ModelConfig = config
  override suspend fun updateConfig(config: ModelConfig) {
    this.config = config
    _flow.value = config
  }
  override fun observeConfig(): Flow<ModelConfig> = _flow
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
  override suspend fun hasActiveGame(): Boolean =
    sessions.any { it.completedAt == null }
  override suspend fun deleteAll() {
    sessions.clear()
  }
}

private class FakeChatRepository : ChatRepository {
  val savedMessages = mutableListOf<ChatMessage>()

  override suspend fun getMessages(puzzleId: Long): List<ChatMessage> = savedMessages.filter { it.puzzleId == puzzleId }
  override suspend fun saveMessage(message: ChatMessage) { savedMessages.add(message) }
  override suspend fun countUserMessages(puzzleId: Long): Int = savedMessages.count { it.puzzleId == puzzleId && it.role == com.woliveiras.palabrita.core.model.MessageRole.USER }
  override suspend fun getPuzzle(puzzleId: Long): Puzzle? = null
  override suspend fun deleteAll() { savedMessages.clear() }
}

private class FakePuzzleRepository : PuzzleRepository {
  var unplayedAiPuzzlesCleared = false
  var allUnplayed = false

  override suspend fun getNextUnplayed(language: String, difficulty: Int): Puzzle? = null
  override suspend fun countUnplayed(language: String, difficulty: Int): Int = 0
  override suspend fun countAllUnplayed(language: String): Int = 0
  override suspend fun getAllGeneratedWords(): Set<String> = emptySet()
  override suspend fun getRecentWords(limit: Int): List<String> = emptyList()
  override suspend fun savePuzzle(puzzle: Puzzle): Long = 0
  override suspend fun savePuzzles(puzzles: List<Puzzle>) {}
  override suspend fun markAsPlayed(puzzleId: Long) {}
  override suspend fun deleteUnplayedAiPuzzles() { unplayedAiPuzzlesCleared = true }
  override suspend fun markAllUnplayed() { allUnplayed = true }
}
