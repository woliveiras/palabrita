package com.woliveiras.palabrita.feature.onboarding

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.worker.GenerationWorkState
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.testing.FakeAppPreferences
import com.woliveiras.palabrita.core.testing.FakeGenerationScheduler
import com.woliveiras.palabrita.core.testing.FakeLlmEngineManager
import com.woliveiras.palabrita.core.testing.FakeModelRepository
import com.woliveiras.palabrita.core.testing.FakePuzzleGenerator
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

  // --- Regression: "Start Playing" appeared before generation finished ---

  /**
   * Reproduces the race condition where WorkManager's stale SUCCEEDED state from the onboarding
   * generation was visible when the ViewModel started observing. Because triggerGeneration() sets
   * hasTriggered=true before the new work reaches RUNNING, the old SUCCEEDED was mistakenly
   * accepted and isComplete was set to true immediately.
   *
   * Fix: isComplete must NOT become true unless RUNNING was observed first for this trigger.
   */
  @Test
  fun `isComplete stays false when stale SUCCEEDED arrives before new work starts RUNNING`() =
    runTest {
      val scheduler = FakeGenerationScheduler()
      // Simulate stale state: previous generation already SUCCEEDED in WorkManager
      scheduler.emit(GenerationWorkState.SUCCEEDED, generatedCount = 5, totalExpected = 5)

      val vm = createViewModel(scheduler = scheduler)

      // Trigger regeneration (as the user would after playing all puzzles)
      vm.triggerGeneration(ModelId.QWEN3_0_6B)
      advanceUntilIdle()

      // The stale SUCCEEDED must NOT show "Start Playing"
      assertThat(vm.state.value.isComplete).isFalse()
      assertThat(vm.state.value.isGenerating).isTrue()
    }

  /**
   * Verifies the normal happy path: RUNNING followed by SUCCEEDED does complete generation.
   */
  @Test
  fun `isComplete becomes true only after observing RUNNING then SUCCEEDED`() = runTest {
    val scheduler = FakeGenerationScheduler()
    val vm = createViewModel(scheduler = scheduler)

    vm.triggerGeneration(ModelId.QWEN3_0_6B)
    advanceUntilIdle()

    // Worker starts running
    scheduler.emit(GenerationWorkState.RUNNING, generatedCount = 0, totalExpected = 10)
    advanceUntilIdle()

    assertThat(vm.state.value.isComplete).isFalse()
    assertThat(vm.state.value.isGenerating).isTrue()

    // Worker finishes successfully with real puzzles
    scheduler.emit(GenerationWorkState.SUCCEEDED, generatedCount = 9, totalExpected = 10)
    advanceUntilIdle()

    assertThat(vm.state.value.isComplete).isTrue()
    assertThat(vm.state.value.failed).isFalse()
    assertThat(vm.state.value.progress.generatedCount).isEqualTo(9)
  }

  /**
   * Verifies that a SUCCEEDED with generatedCount=0 (all LLM retries failed) is shown as a
   * failure, not as "generation complete". This prevents the user from seeing "Start Playing"
   * with 0 new puzzles.
   */
  @Test
  fun `SUCCEEDED with zero puzzles is treated as failure`() = runTest {
    val scheduler = FakeGenerationScheduler()
    val vm = createViewModel(scheduler = scheduler)

    vm.triggerGeneration(ModelId.QWEN3_0_6B)
    advanceUntilIdle()

    scheduler.emit(GenerationWorkState.RUNNING, generatedCount = 0, totalExpected = 10)
    advanceUntilIdle()

    scheduler.emit(GenerationWorkState.SUCCEEDED, generatedCount = 0, totalExpected = 10)
    advanceUntilIdle()

    assertThat(vm.state.value.isComplete).isFalse()
    assertThat(vm.state.value.failed).isTrue()
  }

  /**
   * Verifies that a SUCCEEDED with totalExpected=0 (worker skipped because threshold was already
   * met) is treated as complete, not a failure. This is the normal "enough puzzles" early-exit.
   */
  @Test
  fun `SUCCEEDED with totalExpected zero is treated as complete`() = runTest {
    val scheduler = FakeGenerationScheduler()
    val vm = createViewModel(scheduler = scheduler)

    vm.triggerGeneration(ModelId.QWEN3_0_6B)
    advanceUntilIdle()

    scheduler.emit(GenerationWorkState.RUNNING, generatedCount = 0, totalExpected = 0)
    advanceUntilIdle()

    scheduler.emit(GenerationWorkState.SUCCEEDED, generatedCount = 0, totalExpected = 0)
    advanceUntilIdle()

    assertThat(vm.state.value.isComplete).isTrue()
    assertThat(vm.state.value.failed).isFalse()
  }

  /**
   * Verifies that FAILED from the worker sets failed=true and not complete.
   */
  @Test
  fun `FAILED state sets failed and keeps isComplete false`() = runTest {
    val scheduler = FakeGenerationScheduler()
    val vm = createViewModel(scheduler = scheduler)

    vm.triggerGeneration(ModelId.QWEN3_0_6B)
    advanceUntilIdle()

    scheduler.emit(GenerationWorkState.RUNNING, generatedCount = 0, totalExpected = 10)
    advanceUntilIdle()

    scheduler.emit(GenerationWorkState.FAILED)
    advanceUntilIdle()

    assertThat(vm.state.value.isComplete).isFalse()
    assertThat(vm.state.value.failed).isTrue()
  }

  /**
   * Verifies that FAILED without seeing RUNNING first (another stale-state scenario) is ignored.
   */
  @Test
  fun `stale FAILED state before RUNNING is ignored`() = runTest {
    val scheduler = FakeGenerationScheduler()
    scheduler.emit(GenerationWorkState.FAILED)

    val vm = createViewModel(scheduler = scheduler)

    vm.triggerGeneration(ModelId.QWEN3_0_6B)
    advanceUntilIdle()

    // Stale FAILED should not surface
    assertThat(vm.state.value.failed).isFalse()
    assertThat(vm.state.value.isGenerating).isTrue()
  }

  // --- Helpers ---

  private fun createViewModel(
    scheduler: FakeGenerationScheduler = FakeGenerationScheduler(),
    engineManager: FakeLlmEngineManager = FakeLlmEngineManager(EngineState.Ready),
  ): GenerationViewModel =
    GenerationViewModel(
      generationScheduler = scheduler,
      appPreferences = FakeAppPreferences(),
      modelRepository =
        FakeModelRepository(
          ModelConfig(modelId = ModelId.QWEN3_0_6B)
        ),
      engineManager = engineManager,
      puzzleGenerator = FakePuzzleGenerator(),
    )
}
