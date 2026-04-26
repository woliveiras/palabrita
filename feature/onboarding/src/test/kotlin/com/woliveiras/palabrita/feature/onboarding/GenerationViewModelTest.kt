package com.woliveiras.palabrita.feature.onboarding

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.GenerationResult
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.testing.FakeAppPreferences
import com.woliveiras.palabrita.core.testing.FakeGeneratePuzzlesUseCase
import com.woliveiras.palabrita.core.testing.FakeLlmEngineManager
import com.woliveiras.palabrita.core.testing.FakeModelRepository
import com.woliveiras.palabrita.core.testing.FakePuzzleGenerator
import com.woliveiras.palabrita.core.testing.FakeStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GenerationViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Happy path ---

  @Test
  fun `isComplete becomes true after successful generation`() = runTest {
    val useCase = FakeGeneratePuzzlesUseCase(GenerationResult(generatedCount = 5, batchSize = 5))
    val vm = createViewModel(useCase = useCase)

    vm.triggerGeneration(ModelId.QWEN3_0_6B)
    advanceUntilIdle()

    assertThat(vm.state.value.isComplete).isTrue()
    assertThat(vm.state.value.failed).isFalse()
    assertThat(vm.state.value.isGenerating).isFalse()
  }

  @Test
  fun `progress is updated during generation`() = runTest {
    val useCase = FakeGeneratePuzzlesUseCase(GenerationResult(generatedCount = 9, batchSize = 10))
    val vm = createViewModel(useCase = useCase)

    vm.triggerGeneration(ModelId.QWEN3_0_6B)
    advanceUntilIdle()

    assertThat(vm.state.value.progress.generatedCount).isEqualTo(9)
    assertThat(vm.state.value.progress.totalExpected).isEqualTo(10)
  }

  @Test
  fun `generation skipped (batchSize -1) is treated as complete`() = runTest {
    val useCase = FakeGeneratePuzzlesUseCase(GenerationResult(generatedCount = 0, batchSize = -1))
    val vm = createViewModel(useCase = useCase)

    vm.triggerGeneration(ModelId.QWEN3_0_6B)
    advanceUntilIdle()

    assertThat(vm.state.value.isComplete).isTrue()
    assertThat(vm.state.value.failed).isFalse()
  }

  // --- Failure paths ---

  @Test
  fun `zero puzzles generated is treated as failure`() = runTest {
    val useCase = FakeGeneratePuzzlesUseCase(GenerationResult(generatedCount = 0, batchSize = 10))
    val vm = createViewModel(useCase = useCase)

    vm.triggerGeneration(ModelId.QWEN3_0_6B)
    advanceUntilIdle()

    assertThat(vm.state.value.isComplete).isFalse()
    assertThat(vm.state.value.failed).isTrue()
  }

  @Test
  fun `ModelId NONE results in failure without calling use case`() = runTest {
    val useCase = FakeGeneratePuzzlesUseCase()
    val vm =
      createViewModel(
        useCase = useCase,
        modelRepository = FakeModelRepository(ModelConfig(modelId = ModelId.NONE)),
      )

    vm.triggerGeneration(null)
    advanceUntilIdle()

    assertThat(vm.state.value.isComplete).isFalse()
    assertThat(vm.state.value.failed).isTrue()
    assertThat(useCase.capturedModelId).isNull()
  }

  @Test
  fun `engine error before generation results in failure`() = runTest {
    val engineManager =
      FakeLlmEngineManager(initialState = EngineState.Error("GPU not supported"))
    // Make initialize() fail by having it set an error state
    val vm = createViewModel(engineManager = engineManager)

    // Simulate engine erroring on initialize
    // FakeLlmEngineManager.initialize() sets state to Ready; we need to verify
    // that if the engine is NOT ready and init fails, failure is surfaced.
    // This test confirms that the ViewModel handles EngineState.Error.
    engineManager.setError("init failed")
    vm.triggerGeneration(ModelId.QWEN3_0_6B)
    advanceUntilIdle()

    assertThat(vm.state.value.failed).isTrue()
    assertThat(vm.state.value.isComplete).isFalse()
  }

  // --- Helpers ---

  private fun createViewModel(
    useCase: FakeGeneratePuzzlesUseCase = FakeGeneratePuzzlesUseCase(),
    engineManager: FakeLlmEngineManager = FakeLlmEngineManager(EngineState.Ready),
    modelRepository: FakeModelRepository = FakeModelRepository(ModelConfig(modelId = ModelId.QWEN3_0_6B)),
  ): GenerationViewModel =
    GenerationViewModel(
      generatePuzzlesUseCase = useCase,
      appPreferences = FakeAppPreferences(),
      modelRepository = modelRepository,
      statsRepository = FakeStatsRepository(),
      engineManager = engineManager,
      puzzleGenerator = FakePuzzleGenerator(),
    )
}

