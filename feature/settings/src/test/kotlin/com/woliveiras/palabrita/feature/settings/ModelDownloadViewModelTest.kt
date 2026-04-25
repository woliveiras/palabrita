package com.woliveiras.palabrita.feature.settings

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.ai.AiModelRegistry
import com.woliveiras.palabrita.core.ai.ModelDownloadManager
import com.woliveiras.palabrita.core.ai.ModelDownloadProgress
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.testing.FakeModelDownloadManager
import com.woliveiras.palabrita.core.testing.FakeModelRepository
import com.woliveiras.palabrita.core.testing.FakePuzzleRepository
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
class ModelDownloadViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Initialization ---

  @Test
  fun `init loads model info from registry`() = runTest {
    val vm = createViewModel(modelId = ModelId.QWEN3_0_6B)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.modelId).isEqualTo(ModelId.QWEN3_0_6B)
    assertThat(vm.state.value.modelInfo).isNotNull()
    assertThat(vm.state.value.modelInfo?.modelId).isEqualTo(ModelId.QWEN3_0_6B)
  }

  @Test
  fun `init with unknown modelId results in null modelInfo`() = runTest {
    val vm = createViewModel(modelId = ModelId.NONE)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.modelId).isEqualTo(ModelId.NONE)
    assertThat(vm.state.value.modelInfo).isNull()
  }

  // --- Download flow ---

  @Test
  fun `StartDownload triggers download manager`() = runTest {
    val downloadManager = FakeModelDownloadManager()
    val vm = createViewModel(downloadManager = downloadManager, modelId = ModelId.QWEN3_0_6B)
    testDispatcher.scheduler.advanceUntilIdle()

    vm.events.test {
      vm.onAction(ModelDownloadUiAction.StartDownload)
      testDispatcher.scheduler.advanceUntilIdle()
      // FakeModelDownloadManager emits Completed immediately on startDownload
      // So we should get a navigation event
      val event = awaitItem()
      assertThat(event).isNotNull()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `download completing with existing puzzles emits NavigateBack`() = runTest {
    val puzzleRepo = FakePuzzleRepository().apply { unplayedCount = 5 }
    val vm = createViewModel(puzzleRepo = puzzleRepo, modelId = ModelId.QWEN3_0_6B)
    testDispatcher.scheduler.advanceUntilIdle()

    vm.events.test {
      vm.onAction(ModelDownloadUiAction.StartDownload)
      testDispatcher.scheduler.advanceUntilIdle()
      val event = awaitItem()
      assertThat(event).isInstanceOf(ModelDownloadUiEvent.NavigateBack::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `download completing with no puzzles emits NavigateToGeneration`() = runTest {
    val puzzleRepo = FakePuzzleRepository().apply { unplayedCount = 0 }
    val vm = createViewModel(puzzleRepo = puzzleRepo, modelId = ModelId.QWEN3_0_6B)
    testDispatcher.scheduler.advanceUntilIdle()

    vm.events.test {
      vm.onAction(ModelDownloadUiAction.StartDownload)
      testDispatcher.scheduler.advanceUntilIdle()
      val event = awaitItem()
      assertThat(event).isInstanceOf(ModelDownloadUiEvent.NavigateToGeneration::class.java)
      assertThat((event as ModelDownloadUiEvent.NavigateToGeneration).modelId)
        .isEqualTo(ModelId.QWEN3_0_6B)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `download completing updates model repository config`() = runTest {
    val modelRepo = FakeModelRepository()
    val vm =
      createViewModel(
        modelRepo = modelRepo,
        puzzleRepo = FakePuzzleRepository().apply { unplayedCount = 1 },
        modelId = ModelId.QWEN3_0_6B,
      )
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ModelDownloadUiAction.StartDownload)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(modelRepo.getConfig().modelId).isEqualTo(ModelId.QWEN3_0_6B)
    assertThat(modelRepo.getConfig().downloadState).isEqualTo(DownloadState.DOWNLOADED)
  }

  @Test
  fun `CancelDownload emits NavigateBack`() = runTest {
    val vm = createViewModel(modelId = ModelId.QWEN3_0_6B)
    testDispatcher.scheduler.advanceUntilIdle()

    vm.events.test {
      vm.onAction(ModelDownloadUiAction.CancelDownload)
      testDispatcher.scheduler.advanceUntilIdle()
      assertThat(awaitItem()).isInstanceOf(ModelDownloadUiEvent.NavigateBack::class.java)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `DismissError clears error message`() = runTest {
    val fakeProgress =
      kotlinx.coroutines.flow.MutableStateFlow<ModelDownloadProgress>(ModelDownloadProgress.Idle)
    val downloadManager =
      object : com.woliveiras.palabrita.core.ai.ModelDownloadManager {
        override val progress: kotlinx.coroutines.flow.StateFlow<ModelDownloadProgress> =
          fakeProgress

        override suspend fun startDownload(modelId: ModelId) {
          fakeProgress.value = ModelDownloadProgress.Failed("network error")
        }

        override fun cancelDownload() {}

        override fun getModelPath(modelId: ModelId): String? = null
      }
    val vm = createViewModel(downloadManager = downloadManager, modelId = ModelId.QWEN3_0_6B)
    testDispatcher.scheduler.advanceUntilIdle()
    vm.onAction(ModelDownloadUiAction.StartDownload)
    testDispatcher.scheduler.advanceUntilIdle()
    assertThat(vm.state.value.errorMessage).isNotNull()

    vm.onAction(ModelDownloadUiAction.DismissError)
    assertThat(vm.state.value.errorMessage).isNull()
  }

  // --- Helpers ---

  private fun createViewModel(
    modelId: ModelId = ModelId.NONE,
    downloadManager: ModelDownloadManager = FakeModelDownloadManager(),
    modelRepo: FakeModelRepository = FakeModelRepository(),
    puzzleRepo: FakePuzzleRepository = FakePuzzleRepository(),
    statsRepo: FakeStatsRepository = FakeStatsRepository(PlayerStats()),
  ) =
    ModelDownloadViewModel(
      savedStateHandle = SavedStateHandle(mapOf("modelId" to modelId.name)),
      downloadManager = downloadManager,
      modelRepository = modelRepo,
      puzzleRepository = puzzleRepo,
      statsRepository = statsRepo,
      modelRegistry = AiModelRegistry,
    )
}
