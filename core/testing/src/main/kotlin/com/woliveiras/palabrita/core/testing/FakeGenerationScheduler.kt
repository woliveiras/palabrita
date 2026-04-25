package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.ai.worker.GenerationInfo
import com.woliveiras.palabrita.core.ai.worker.GenerationProgress
import com.woliveiras.palabrita.core.ai.worker.GenerationWorkState
import com.woliveiras.palabrita.core.ai.worker.PuzzleGenerationScheduler
import com.woliveiras.palabrita.core.model.ModelId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGenerationScheduler : PuzzleGenerationScheduler {
  var scheduledModelId: ModelId? = null
  var lastWorkId: UUID = UUID.randomUUID()

  private val _info = MutableStateFlow(GenerationInfo())

  /**
   * Emit a new work state + optional progress from tests.
   * Automatically includes [lastWorkId] so the ViewModel's workId guard passes.
   */
  fun emit(
    state: GenerationWorkState,
    generatedCount: Int = 0,
    totalExpected: Int = 0,
  ) {
    _info.value =
      GenerationInfo(state, GenerationProgress(generatedCount, totalExpected), lastWorkId)
  }

  override fun scheduleGeneration(modelId: ModelId): UUID {
    scheduledModelId = modelId
    lastWorkId = UUID.randomUUID()
    return lastWorkId
  }

  override fun cancelGeneration() {}

  override fun observeGenerationState(): Flow<GenerationWorkState> =
    MutableStateFlow(GenerationWorkState.IDLE)

  override fun observeGenerationInfo(): Flow<GenerationInfo> = _info
}
