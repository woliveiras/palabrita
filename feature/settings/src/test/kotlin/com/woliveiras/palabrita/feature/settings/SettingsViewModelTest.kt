package com.woliveiras.palabrita.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.testing.FakeModelRepository
import com.woliveiras.palabrita.core.testing.FakeStatsRepository
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
    val stats = PlayerStats(totalPlayed = 42, totalWon = 36)
    val vm = createViewModel(stats = stats)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.stats.totalPlayed).isEqualTo(42)
    assertThat(vm.state.value.stats.totalWon).isEqualTo(36)
  }

  @Test
  fun `loads current model config on init`() = runTest {
    val config =
      ModelConfig(
        modelId = ModelId.GEMMA4_E2B,
        downloadState = DownloadState.DOWNLOADED,
        sizeBytes = 2_600_000_000,
      )
    val vm = createViewModel(modelConfig = config)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.currentModel.modelId).isEqualTo(ModelId.GEMMA4_E2B)
  }

  @Test
  fun `loads all available models on init`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.availableModels).isNotEmpty()
  }

  // --- Model picker ---

  @Test
  fun `ShowModelPicker sets isModelPickerVisible true`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ShowModelPicker)
    assertThat(vm.state.value.isModelPickerVisible).isTrue()
  }

  @Test
  fun `DismissModelPicker sets isModelPickerVisible false`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(SettingsAction.ShowModelPicker)
    vm.onAction(SettingsAction.DismissModelPicker)
    assertThat(vm.state.value.isModelPickerVisible).isFalse()
  }

  @Test
  fun `selecting already-downloaded model updates config and closes picker`() = runTest {
    val config = ModelConfig(modelId = ModelId.QWEN3_0_6B, downloadState = DownloadState.DOWNLOADED)
    val modelRepo = FakeModelRepository(config)
    val vm = createViewModel(modelConfig = config, modelRepo = modelRepo)
    testDispatcher.scheduler.advanceUntilIdle()
    // Select a DIFFERENT model that is also stored in the repo as downloaded
    val secondConfig =
      ModelConfig(modelId = ModelId.GEMMA4_E2B, downloadState = DownloadState.DOWNLOADED)
    modelRepo.updateConfig(secondConfig)
    vm.onAction(SettingsAction.ShowModelPicker)
    vm.onAction(SettingsAction.SelectModel(ModelId.GEMMA4_E2B))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.isModelPickerVisible).isFalse()
    assertThat(vm.state.value.currentModel.modelId).isEqualTo(ModelId.GEMMA4_E2B)
  }

  @Test
  fun `selecting non-downloaded model emits NavigateToModelDownload event`() = runTest {
    val config = ModelConfig(modelId = ModelId.QWEN3_0_6B, downloadState = DownloadState.DOWNLOADED)
    val modelRepo = FakeModelRepository(config)
    val vm = createViewModel(modelConfig = config, modelRepo = modelRepo)
    testDispatcher.scheduler.advanceUntilIdle()

    vm.events.test {
      vm.onAction(SettingsAction.SelectModel(ModelId.GEMMA4_E2B))
      testDispatcher.scheduler.advanceUntilIdle()
      val event = awaitItem()
      assertThat(event).isInstanceOf(SettingsEvent.NavigateToModelDownload::class.java)
      assertThat((event as SettingsEvent.NavigateToModelDownload).modelId)
        .isEqualTo(ModelId.GEMMA4_E2B)
      cancelAndIgnoreRemainingEvents()
    }
  }

  // --- Navigation events ---

  @Test
  fun `RegenPuzzles action emits NavigateToGeneration event`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.events.test {
      vm.onAction(SettingsAction.RegenPuzzles)
      testDispatcher.scheduler.advanceUntilIdle()
      assertThat(awaitItem()).isInstanceOf(SettingsEvent.NavigateToGeneration::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `NavigateToLanguageSelection action emits NavigateToLanguageSelection event`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.events.test {
      vm.onAction(SettingsAction.NavigateToLanguageSelection)
      testDispatcher.scheduler.advanceUntilIdle()
      assertThat(awaitItem()).isInstanceOf(SettingsEvent.NavigateToLanguageSelection::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `NavigateToAiInfo action emits NavigateToAiInfo event`() = runTest {
    val vm = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()
    vm.events.test {
      vm.onAction(SettingsAction.NavigateToAiInfo)
      testDispatcher.scheduler.advanceUntilIdle()
      assertThat(awaitItem()).isInstanceOf(SettingsEvent.NavigateToAiInfo::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  // --- Helpers ---

  private fun createViewModel(
    stats: PlayerStats = PlayerStats(),
    modelConfig: ModelConfig = ModelConfig(),
    deviceTier: DeviceTier = DeviceTier.HIGH,
    statsRepo: FakeStatsRepository = FakeStatsRepository(stats),
    modelRepo: FakeModelRepository = FakeModelRepository(modelConfig),
  ): SettingsViewModel =
    SettingsViewModel(
      statsRepository = statsRepo,
      modelRepository = modelRepo,
      deviceTier = deviceTier,
    )
}
