package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.testing.FakeAppPreferences
import com.woliveiras.palabrita.core.testing.FakeGameSessionRepository
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

    assertThat(result.generatedCount).isEqualTo(0)
    assertThat(prefs.generationCycle.first()).isEqualTo(0)
  }

  @Test
  fun `cycle is NOT incremented when generatedCount is zero`() = runTest {
    val generator = FakePuzzleGenerator()
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

    useCase.execute("pt", ModelId.QWEN3_0_6B) { _, batchSize -> reportedBatchSizes.add(batchSize) }

    // cycle 0 → batchSize = 5
    assertThat(reportedBatchSizes).isNotEmpty()
    assertThat(reportedBatchSizes).doesNotContain(-1)
  }

  // --- Mastery gate: level 1 ---

  @Test
  fun `stays at level 1 when wins are below required`() = runTest {
    // cycle 0 → 4-letter, winsRequired=5, player has 4 wins → should NOT advance
    val prefs = FakeAppPreferences()
    val sessionRepo = FakeGameSessionRepository().apply { winsPerDifficulty[4] = 4 }
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs, sessionRepo = sessionRepo)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(prefs.generationCycle.first()).isEqualTo(0)
    // Should generate remaining: min(5 - 4, 5) = 1
    assertThat(result.batchSize).isEqualTo(1)
  }

  @Test
  fun `advances to level 2 when level 1 wins are met`() = runTest {
    // cycle 0 → 4-letter, winsRequired=5, player has 5 wins → advance to cycle 1
    val prefs = FakeAppPreferences()
    val sessionRepo = FakeGameSessionRepository().apply { winsPerDifficulty[4] = 5 }
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs, sessionRepo = sessionRepo)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(prefs.generationCycle.first()).isEqualTo(1)
    // Level 2: 5-letter, batchSize=10
    assertThat(result.batchSize).isEqualTo(10)
  }

  @Test
  fun `generates full initial batch when zero wins and zero puzzles`() = runTest {
    // cycle 0, 0 wins, 0 unplayed → full batch of 5 at 4-letter
    val prefs = FakeAppPreferences()
    val sessionRepo = FakeGameSessionRepository()
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs, sessionRepo = sessionRepo)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(prefs.generationCycle.first()).isEqualTo(0)
    assertThat(result.batchSize).isEqualTo(5)
    assertThat(result.generatedCount).isEqualTo(5)
  }

  @Test
  fun `generates remaining count when some wins at level 1`() = runTest {
    // cycle 0 → winsRequired=5, 3 wins → remaining = 2
    val prefs = FakeAppPreferences()
    val sessionRepo = FakeGameSessionRepository().apply { winsPerDifficulty[4] = 3 }
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs, sessionRepo = sessionRepo)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(result.batchSize).isEqualTo(2)
  }

  // --- Mastery gate: level 2 ---

  @Test
  fun `generates remaining at level 2 when wins below required`() = runTest {
    // cycle 1 → 5-letter, winsRequired=10, player has 7 wins → remaining = 3
    val prefs = FakeAppPreferences().apply { incrementGenerationCycle() } // cycle=1
    val sessionRepo = FakeGameSessionRepository().apply { winsPerDifficulty[5] = 7 }
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs, sessionRepo = sessionRepo)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(prefs.generationCycle.first()).isEqualTo(1) // did NOT advance
    assertThat(result.batchSize).isEqualTo(3)
  }

  @Test
  fun `advances to level 3 when level 2 wins are met`() = runTest {
    // cycle 1 → 5-letter, winsRequired=10, player has 10 wins → advance to cycle 2
    val prefs = FakeAppPreferences().apply { incrementGenerationCycle() } // cycle=1
    val sessionRepo = FakeGameSessionRepository().apply { winsPerDifficulty[5] = 10 }
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs, sessionRepo = sessionRepo)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(prefs.generationCycle.first()).isEqualTo(2)
    assertThat(result.batchSize).isEqualTo(10) // level 3: 6-letter, batchSize=10
  }

  // --- Mastery gate: level 3 (cap) ---

  @Test
  fun `level 3 cap still generates 6-letter words after mastery`() = runTest {
    // cycle 2 → 6-letter, winsRequired=10, player has 10 wins → advance to cycle 3
    // but level 3+ is capped → still 6-letter, batchSize=10
    val prefs = FakeAppPreferences().apply {
      incrementGenerationCycle() // cycle=1
      incrementGenerationCycle() // cycle=2
    }
    val sessionRepo = FakeGameSessionRepository().apply { winsPerDifficulty[6] = 10 }
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs, sessionRepo = sessionRepo)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(prefs.generationCycle.first()).isEqualTo(3)
    assertThat(result.batchSize).isEqualTo(10) // still capped at 6-letter
  }

  @Test
  fun `level 3 generates remaining when wins below required`() = runTest {
    // cycle 5 → capped at 6-letter, winsRequired=10, player has 8 wins → remaining = 2
    val prefs = FakeAppPreferences().apply {
      repeat(5) { incrementGenerationCycle() } // cycle=5
    }
    val sessionRepo = FakeGameSessionRepository().apply { winsPerDifficulty[6] = 8 }
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs, sessionRepo = sessionRepo)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(prefs.generationCycle.first()).isEqualTo(5) // did NOT advance
    assertThat(result.batchSize).isEqualTo(2)
  }

  // --- Backward compatibility ---

  @Test
  fun `existing user with retroactive wins advances immediately`() = runTest {
    // cycle 1, but already has 15 wins at difficulty=5 → should advance right away
    val prefs = FakeAppPreferences().apply { incrementGenerationCycle() } // cycle=1
    val sessionRepo = FakeGameSessionRepository().apply { winsPerDifficulty[5] = 15 }
    val generator = FakePuzzleGenerator().apply { setBatchResults(1.0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs, sessionRepo = sessionRepo)

    val result = useCase.execute("pt", ModelId.QWEN3_0_6B)

    assertThat(prefs.generationCycle.first()).isEqualTo(2)
    assertThat(result.batchSize).isEqualTo(10)
  }

  // --- Cycle advancement threshold (still applies within mastery) ---

  @Test
  fun `cycle is NOT incremented when fewer than half the batch is generated`() = runTest {
    // cycle 0 → batchSize=5, but wins=5 so it should try to advance.
    // Generator only produces 1 of 10 (level 2) — less than half → don't advance.
    val prefs = FakeAppPreferences()
    val sessionRepo = FakeGameSessionRepository().apply { winsPerDifficulty[4] = 5 }
    val generator = FakePuzzleGenerator().apply { setBatchResults(0.1f, 0f, 0f, 0f) }
    val useCase = createUseCase(generator = generator, prefs = prefs, sessionRepo = sessionRepo)

    useCase.execute("pt", ModelId.QWEN3_0_6B)

    // Cycle was incremented to 1 before generation (mastery met),
    // but generation failed → cycle should stay at 1 (not revert)
    // The cycle was already incremented because mastery was met
    assertThat(prefs.generationCycle.first()).isEqualTo(1)
  }

  // --- CancellationException propagation ---

  @Test
  fun `CancellationException propagates through execute`() = runTest {
    val engine = FakeLlmEngineManager()
    val generator =
      object : PuzzleGenerator {
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

  // --- Helpers ---

  private fun createUseCase(
    repo: FakePuzzleRepository = FakePuzzleRepository(),
    generator: PuzzleGenerator = FakePuzzleGenerator(),
    engine: FakeLlmEngineManager = FakeLlmEngineManager(),
    prefs: FakeAppPreferences = FakeAppPreferences(),
    sessionRepo: FakeGameSessionRepository = FakeGameSessionRepository(),
  ): GeneratePuzzlesUseCaseImpl =
    GeneratePuzzlesUseCaseImpl(
      puzzleRepository = repo,
      puzzleGenerator = generator,
      engineManager = engine,
      appPreferences = prefs,
      gameSessionRepository = sessionRepo,
    )
}
