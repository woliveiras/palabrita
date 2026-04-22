package com.woliveiras.palabrita.core.ai.worker

import com.woliveiras.palabrita.core.model.ModelId
import kotlinx.coroutines.flow.Flow

enum class GenerationWorkState {
  RUNNING,
  SUCCEEDED,
  FAILED,
  IDLE,
}

interface PuzzleGenerationScheduler {
  fun scheduleGeneration(modelId: ModelId)

  fun observeGenerationState(): Flow<GenerationWorkState>
}
