package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.testing.FakeAppPreferences
import com.woliveiras.palabrita.core.testing.FakeLlmEngineManager
import com.woliveiras.palabrita.core.testing.FakePuzzleGenerator
import com.woliveiras.palabrita.core.testing.FakePuzzleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GeneratePuzzlesUseCaseTest {

  // --- Skip (threshold already met) ---

  @Test
  fun `returns skipped result when unplayed count is above threshold`() = runTest {
    val repo = FakePuzzleRepository().apply { unplayedCount = 5 }
    val useCase = createUseCase(repo = repo)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(result.batchSize).isEqualTo(-1)
    assertThat(result.generatedCount).isEqualTo(0)
  }

  @Test
  fun `does not call generator when unplayed count is above threshold`() = runTest {
    val repo = FakePuzzleRepository().apply { unplayedCount = 10 }
    val generator = FakePuzzleGenerator()
    val useCase = createUseCase(repo = repo, generator = generator)

    useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(generator.callCount).isEqualTo(0)
  }

  // --- Happy path ---

  @Test
  fun `returns full batch when generator succeeds on first attempt`() = runTest {
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val prefs = FakeAppPreferences()
    val useCase = createUseCase(generator = generator, prefs = prefs)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    // cycle 0 → batchSize = 5
    assertThat(result.generatedCount).isEqualTo(5)
    assertThat(result.batchSize).isEqualTo(5)
  }

  @Test
  fun `cycle is incremented when at least one puzzle is generated`() = runTest {
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val prefs = FakeAppPreferences()
    val useCase = createUseCase(generator = generator, prefs = prefs)

    useCase.execute("pt", ModelId.QWEN3_0_6B)

    // Collect once to read the updated value
    assertThat(prefs.generationCycle.first()).isEqualTo(1)
  }

  @Test
  fun `puzzles are persisted to the repository`() = runTest {
    val repo = FakePuzzleRepository()
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val useCase = createUseCase(repo = repo, generator = generator)

    useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(repo.savedPuzzles).isNotEmpty()
  }

  // --- Partial batch / retry ---

  @Test
  fun `retry fills missing slots when first batch is partial`() = runTest {
    // First call: 80% (4 of 5), second call: 100% of remaining (1 of 1)
    val generator = FakePuzzleGenerator().apply { setBatchResults(0.8f, 1.0f) }
    val repo = FakePuzzleRepository()
    val useCase = createUseCase(repo = repo, generator = generator)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(result.generatedCount).isEqualTo(5)
    assertThat(generator.callCount).isEqualTo(2)
  }

  @Test
  fun `retry loop runs multiple passes until batch is complete`() = runTest {
    // cycle 0 → batchSize=5
    // pass 0: 3 of 5 (60%), pass 1: 1 of 2 (50%), pass 2: 1 of 1 (100%)
    val generator = FakePuzzleGenerator().apply { setBatchResults(0.6f, 0.5f, 1.0f) }
    val repo = FakePuzzleRepository()
    val useCase = createUseCase(repo = repo, generator = generator)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(result.generatedCount).isEqualTo(5)
    assertThat(generator.callCount).isEqualTo(3)
  }

  @Test
  fun `stops retrying after MAX_BATCH_RETRY_PASSES even if batch is incomplete`() = runTest {
    // All calls return 0 — LLM consistently fails
    val generator = FakePuzzleGenerator().apply { setBatchResults(0f, 0f, 0f, 0f) }
    val prefs = FakeAppPreferences()
    val useCase = createUseCase(generator = generator, prefs = prefs)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    // 1 initial call + 3 retry passes = 4 total, but since 0 puzzles ever generated,
    // after first call generatedCount=0 and retries stop only after MAX_BATCH_RETRY_PASSES.
    // generatedCount stays 0 → cycle must NOT be incremented.
    assertThat(result.generatedCount).isEqualTo(0)
    assertThat(prefs.generationCycle.first()).isEqualTo(0)
  }

  @Test
  fun `cycle is NOT incremented when generatedCount is zero`() = runTest {
    val generator = FakePuzzleGenerator() // returns empty list (all fractions 0f by default)
    val prefs = FakeAppPreferences()
    val useCase = createUseCase(generator = generator, prefs = prefs)

    useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(prefs.generationCycle.first()).isEqualTo(0)
  }

  @Test
  fun `partial result is returned when retries partially succeed`() = runTest {
    // cycle 0 → batchSize=5; first call: 3/5, retries all produce 0
    val generator = FakePuzzleGenerator().apply { setBatchResults(0.6f, 0f, 0f, 0f) }
    val useCase = createUseCase(generator = generator)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(result.generatedCount).isEqualTo(3)
    assertThat(result.batchSize).isEqualTo(5)
  }

  // --- Engine not ready ---

  @Test
  fun `throws when engine is not ready`() = runTest {
    val engine = FakeLlmEngineManager(initialState = EngineState.Error("not initialized"))
    val useCase = createUseCase(engine = engine)

    var threw = false
    try {
      useCase.execute("pt", ModelId.QWEN3_0_6B)
    } catch (e: IllegalArgumentException) {
      threw = true
    }
    assertThat(threw).isTrue()
  }

  // --- onProgress callback ---

  @Test
  fun `onProgress is called with correct batchSize during generation`() = runTest {
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val reportedBatchSizes = mutableListOf<Int>()
    val useCase = createUseCase(generator = generator)

    useCase.execute("pt", ModelId.QWEN3_0_6B) { _, batchSize ->
      reportedBatchSizes.add(batchSize)
    }

    // cycle 0 → batchSize = 5
    assertThat(reportedBatchSizes).isNotEmpty()
    assertThat(reportedBatchSizes).doesNotContain(-1)
  }

  // --- Helpers ---

  private fun createUseCase(
    repo: FakePuzzleRepository = FakePuzzleRepository(),
    generator: PuzzleGenerator = FakePuzzleGenerator(),
    engine: FakeLlmEngineManager = FakeLlmEngineManager(),
    prefs: FakeAppPreferences = FakeAppPreferences(),
  ): GeneratePuzzlesUseCaseImpl =
    GeneratePuzzlesUseCaseImpl(
      puzzleRepository = repo,
      puzzleGenerator = generator,
      engineManager = engine,
      appPreferences = prefs,
    )

  // --- Cycle advancement threshold ---

  @Test
  fun `cycle is NOT incremented when fewer than half the batch is generated`() = runTest {
    // cycle 0 → batchSize=5, threshold=2; generate only 1 (20% of batch)
    val prefs = FakeAppPreferences()
    val generator = FakePuzzleGenerator().apply { setBatchResults(0.2f, 0f, 0f, 0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs)

    useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(prefs.generationCycle.first()).isEqualTo(0)
  }

  // --- CancellationException propagation ---

  @Test
  fun `CancellationException propagates through execute`() = runTest {
    val engine = FakeLlmEngineManager()
    val generator = object : PuzzleGenerator {
      override val activity: kotlinx.coroutines.flow.StateFlow<GenerationActivity?> =
        kotlinx.coroutines.flow.MutableStateFlow(null)

      override suspend fun generateBatch(
        count: Int,
        language: String,
        wordLength: Int,
        recentWords: List<String>,
        allExistingWords: Set<String>,
        modelId: ModelId,
        onPuzzleAttempted: suspend (Int) -> Unit,
      ): List<com.woliveiras.palabrita.core.model.Puzzle> {
        throw kotlinx.coroutines.CancellationException("test cancel")
      }
    }
    val useCase = createUseCase(engine = engine, generator = generator)

    var threw = false
    try {
      useCase.execute("pt", ModelId.QWEN3_0_6B)
    } catch (e: kotlinx.coroutines.CancellationException) {
      threw = true
    }
    assertThat(threw).isTrue()
  }
}
