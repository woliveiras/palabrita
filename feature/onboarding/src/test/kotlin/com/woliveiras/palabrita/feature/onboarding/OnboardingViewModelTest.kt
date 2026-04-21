package com.woliveiras.palabrita.feature.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.LlmSession
import com.woliveiras.palabrita.core.ai.ModelDownloadManager
import com.woliveiras.palabrita.core.ai.ModelDownloadProgress
import com.woliveiras.palabrita.core.ai.PuzzleGenerator
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.data.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

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
  fun `initial step is WELCOME`() = runTest {
    val vm = createViewModel(deviceTier = DeviceTier.HIGH)
    assertThat(vm.state.value.currentStep).isEqualTo(OnboardingStep.WELCOME)
  }

  @Test
  fun `device tier is set from DeviceCapabilities`() = runTest {
    val vm = createViewModel(deviceTier = DeviceTier.MEDIUM)
    assertThat(vm.state.value.deviceTier).isEqualTo(DeviceTier.MEDIUM)
  }

  // --- Navigation ---

  @Test
  fun `WELCOME next navigates to LANGUAGE`() = runTest {
    val vm = createViewModel()
    vm.onAction(OnboardingAction.Next)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentStep).isEqualTo(OnboardingStep.LANGUAGE)
  }

  @Test
  fun `LANGUAGE next navigates to MODEL_SELECTION`() = runTest {
    val vm = createViewModel()
    vm.onAction(OnboardingAction.Next) // → LANGUAGE
    vm.onAction(OnboardingAction.SelectLanguage("pt"))
    vm.onAction(OnboardingAction.Next) // → MODEL_SELECTION
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentStep).isEqualTo(OnboardingStep.MODEL_SELECTION)
  }

  @Test
  fun `LANGUAGE back navigates to WELCOME`() = runTest {
    val vm = createViewModel()
    vm.onAction(OnboardingAction.Next) // → LANGUAGE
    vm.onAction(OnboardingAction.Back)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentStep).isEqualTo(OnboardingStep.WELCOME)
  }

  @Test
  fun `MODEL_SELECTION back navigates to LANGUAGE`() = runTest {
    val vm = createViewModel()
    vm.onAction(OnboardingAction.Next) // → LANGUAGE
    vm.onAction(OnboardingAction.Next) // → MODEL_SELECTION
    vm.onAction(OnboardingAction.Back)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentStep).isEqualTo(OnboardingStep.LANGUAGE)
  }

  // --- Language selection ---

  @Test
  fun `selecting language updates state`() = runTest {
    val vm = createViewModel()
    vm.onAction(OnboardingAction.SelectLanguage("en"))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.selectedLanguage).isEqualTo("en")
  }

  // --- Model selection ---

  @Test
  fun `auto-select picks GEMMA4_E2B for HIGH tier`() = runTest {
    val vm = createViewModel(deviceTier = DeviceTier.HIGH)
    vm.onAction(OnboardingAction.AutoSelectModel)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.selectedModel).isEqualTo(ModelId.GEMMA4_E2B)
  }

  @Test
  fun `auto-select picks GEMMA3_1B for MEDIUM tier`() = runTest {
    val vm = createViewModel(deviceTier = DeviceTier.MEDIUM)
    vm.onAction(OnboardingAction.AutoSelectModel)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.selectedModel).isEqualTo(ModelId.GEMMA3_1B)
  }

  @Test
  fun `manual model selection updates state`() = runTest {
    val vm = createViewModel(deviceTier = DeviceTier.HIGH)
    vm.onAction(OnboardingAction.SelectModel(ModelId.GEMMA3_1B))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.selectedModel).isEqualTo(ModelId.GEMMA3_1B)
  }

  @Test
  fun `selecting model above tier shows warning`() = runTest {
    val vm = createViewModel(deviceTier = DeviceTier.MEDIUM)
    vm.onAction(OnboardingAction.SelectModel(ModelId.GEMMA4_E2B))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.showTierWarning).isTrue()
  }

  @Test
  fun `selecting model within tier does not show warning`() = runTest {
    val vm = createViewModel(deviceTier = DeviceTier.HIGH)
    vm.onAction(OnboardingAction.SelectModel(ModelId.GEMMA4_E2B))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.showTierWarning).isFalse()
  }

  // --- LOW tier skips to Light mode ---

  @Test
  fun `LOW tier skip-to-light completes onboarding`() = runTest {
    val vm = createViewModel(deviceTier = DeviceTier.LOW)
    vm.onAction(OnboardingAction.Next) // → LANGUAGE
    vm.onAction(OnboardingAction.Next) // → MODEL_SELECTION
    vm.onAction(OnboardingAction.SkipToLightMode)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentStep).isEqualTo(OnboardingStep.COMPLETE)
    assertThat(vm.state.value.selectedModel).isEqualTo(ModelId.NONE)
  }

  // --- State observation via Flow ---

  @Test
  fun `state flow emits updates`() = runTest {
    val vm = createViewModel()
    vm.state.test {
      assertThat(awaitItem().currentStep).isEqualTo(OnboardingStep.WELCOME)
      vm.onAction(OnboardingAction.Next)
      assertThat(awaitItem().currentStep).isEqualTo(OnboardingStep.LANGUAGE)
      cancelAndIgnoreRemainingEvents()
    }
  }

  // --- Helpers ---

  private fun createViewModel(
    deviceTier: DeviceTier = DeviceTier.HIGH,
  ): OnboardingViewModel =
    OnboardingViewModel(
      deviceTier = deviceTier,
      statsRepository = FakeStatsRepository(),
      modelRepository = FakeModelRepository(),
      puzzleRepository = FakePuzzleRepository(),
      appPreferences = FakeAppPreferences(),
      downloadManager = FakeModelDownloadManager(),
      engineManager = FakeLlmEngineManager(),
      puzzleGenerator = FakePuzzleGenerator(),
    )
}

private class FakeStatsRepository : StatsRepository {
  private var stats = PlayerStats()

  override suspend fun getStats(): PlayerStats = stats

  override suspend fun updateAfterGame(won: Boolean, attempts: Int, difficulty: Int, hintsUsed: Int) {
    // no-op for onboarding tests
  }

  override suspend fun checkAndPromoteDifficulty(): Int = stats.currentDifficulty

  override suspend fun updateLanguage(language: String) {}

  override suspend fun updateWordSizePreference(preference: String) {}

  override suspend fun resetProgress() {}

  override fun observeStats(): Flow<PlayerStats> = flowOf(stats)
}

private class FakeModelRepository : ModelRepository {
  private var config = ModelConfig()

  override suspend fun getConfig(): ModelConfig = config

  override suspend fun updateConfig(config: ModelConfig) {
    this.config = config
  }

  override fun observeConfig(): Flow<ModelConfig> = flowOf(config)
}

private class FakeAppPreferences : AppPreferences {
  private val _isOnboardingComplete = MutableStateFlow(false)
  override val isOnboardingComplete: Flow<Boolean> = _isOnboardingComplete

  override suspend fun setOnboardingComplete() {
    _isOnboardingComplete.value = true
  }
}

private class FakePuzzleRepository : PuzzleRepository {
  private val puzzles = mutableListOf<Puzzle>()

  override suspend fun getNextUnplayed(language: String, difficulty: Int): Puzzle? =
    puzzles.firstOrNull { !it.isPlayed && it.language == language && it.difficulty == difficulty }

  override suspend fun countUnplayed(language: String, difficulty: Int): Int =
    puzzles.count { !it.isPlayed && it.language == language && it.difficulty == difficulty }

  override suspend fun getAllGeneratedWords(): Set<String> = puzzles.map { it.word }.toSet()

  override suspend fun getRecentWords(limit: Int): List<String> =
    puzzles.takeLast(limit).map { it.word }

  override suspend fun savePuzzle(puzzle: Puzzle): Long {
    puzzles.add(puzzle)
    return puzzles.size.toLong()
  }

  override suspend fun markAsPlayed(puzzleId: Long) {}

  override suspend fun deleteUnplayedAiPuzzles() {}

  override suspend fun markAllUnplayed() {}
}

private class FakeModelDownloadManager : ModelDownloadManager {
  private val _progress = MutableStateFlow<ModelDownloadProgress>(ModelDownloadProgress.Idle)
  override val progress: StateFlow<ModelDownloadProgress> = _progress

  override suspend fun startDownload(modelId: ModelId) {
    _progress.value = ModelDownloadProgress.Completed("/fake/model.litertlm")
  }

  override fun cancelDownload() {
    _progress.value = ModelDownloadProgress.Idle
  }

  override fun getModelPath(modelId: ModelId): String? = null
}

private class FakeLlmEngineManager : LlmEngineManager {
  private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
  override val engineState: StateFlow<EngineState> = _state

  override suspend fun initialize(modelPath: String) {
    _state.value = EngineState.Ready
  }

  override suspend fun generateSingleTurn(systemPrompt: String?, userPrompt: String): String =
    """{"word":"teste","category":"test","difficulty":1,"hints":["h1","h2","h3","h4","h5"]}"""

  override suspend fun createChatSession(systemPrompt: String): LlmSession =
    object : LlmSession {
      override suspend fun sendMessage(message: String): String = "response"
      override fun sendMessageStreaming(message: String): Flow<String> = flowOf("response")
      override fun close() {}
    }

  override fun destroy() {
    _state.value = EngineState.Uninitialized
  }

  override fun isReady(): Boolean = _state.value is EngineState.Ready
}

private class FakePuzzleGenerator : PuzzleGenerator {
  override suspend fun generateBatch(
    count: Int,
    language: String,
    targetDifficulty: Int,
    recentWords: List<String>,
    allExistingWords: Set<String>,
    modelId: ModelId,
  ): List<Puzzle> = List(count) { i ->
    Puzzle(
      word = "teste$i",
      wordDisplay = "TESTE$i",
      language = language,
      difficulty = targetDifficulty,
      category = "test",
      hints = listOf("h1", "h2", "h3", "h4", "h5"),
      source = com.woliveiras.palabrita.core.model.PuzzleSource.AI,
      generatedAt = System.currentTimeMillis(),
    )
  }
}
