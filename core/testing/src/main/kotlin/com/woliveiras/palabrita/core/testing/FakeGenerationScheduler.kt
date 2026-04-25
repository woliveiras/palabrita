package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.ai.worker.GenerationInfo
import com.woliveiras.palabrita.core.ai.worker.GenerationProgress
import com.woliveiras.palabrita.core.ai.worker.GenerationWorkState
import com.woliveiras.palabrita.core.ai.worker.PuzzleGenerationScheduler
import com.woliveiras.palabrita.core.model.ModelId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGenerationScheduler : PuzzleGenerationScheduler {
  var scheduledModelId: ModelId? = null

  private val _info = MutableStateFlow(GenerationInfo())

  /** Emit a new work state + optional progress from tests. */
  fun emit(
    state: GenerationWorkState,
    generatedCount: Int = 0,
    totalExpected: Int = 0,
  ) {
    _info.value = GenerationInfo(state, GenerationProgress(generatedCount, totalExpected))
  }

  override fun scheduleGeneration(modelId: ModelId) {
    scheduledModelId = modelId
  }

  override fun cancelGeneration() {}

  override fun observeGenerationState(): Flow<GenerationWorkState> =
    MutableStateFlow(GenerationWorkState.IDLE)

  override fun observeGenerationInfo(): Flow<GenerationInfo> = _info
}
