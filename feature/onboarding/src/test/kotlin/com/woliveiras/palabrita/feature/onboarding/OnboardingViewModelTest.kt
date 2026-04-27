package com.woliveiras.palabrita.feature.onboarding

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.ModelId
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
  fun `auto-select picks QWEN3_0_6B for MEDIUM tier`() = runTest {
    val vm = createViewModel(deviceTier = DeviceTier.MEDIUM)
    vm.onAction(OnboardingAction.AutoSelectModel)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.selectedModel).isEqualTo(ModelId.QWEN3_0_6B)
  }

  @Test
  fun `manual model selection updates state`() = runTest {
    val vm = createViewModel(deviceTier = DeviceTier.HIGH)
    vm.onAction(OnboardingAction.SelectModel(ModelId.QWEN3_0_6B))
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.selectedModel).isEqualTo(ModelId.QWEN3_0_6B)
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

  private fun createViewModel(deviceTier: DeviceTier = DeviceTier.HIGH): OnboardingViewModel =
    OnboardingViewModel(
      deviceTier = deviceTier,
      datasetRegistry = com.woliveiras.palabrita.core.ai.DatasetRegistry(),
      statsRepository = FakeStatsRepository(),
      modelRepository = FakeModelRepository(),
      appPreferences = FakeAppPreferences(),
      downloadManager = FakeModelDownloadManager(),
      engineManager = FakeLlmEngineManager(),
    )
}
