package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.ai.worker.GenerationInfo
import com.woliveiras.palabrita.core.ai.worker.GenerationWorkState
import com.woliveiras.palabrita.core.ai.worker.PuzzleGenerationScheduler
import com.woliveiras.palabrita.core.model.ModelId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeGenerationScheduler : PuzzleGenerationScheduler {
  var scheduledModelId: ModelId? = null

  override fun scheduleGeneration(modelId: ModelId) {
    scheduledModelId = modelId
  }

  override fun cancelGeneration() {}

  override fun observeGenerationState(): Flow<GenerationWorkState> =
    flowOf(GenerationWorkState.IDLE)

  override fun observeGenerationInfo(): Flow<GenerationInfo> = flowOf(GenerationInfo())
}
