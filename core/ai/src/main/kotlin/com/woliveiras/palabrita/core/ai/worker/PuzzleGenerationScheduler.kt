package com.woliveiras.palabrita.core.ai.worker

import com.woliveiras.palabrita.core.model.ModelId
import kotlinx.coroutines.flow.Flow

enum class GenerationWorkState {
  RUNNING,
  SUCCEEDED,
  FAILED,
  IDLE,
}

data class GenerationProgress(val generatedCount: Int = 0, val totalExpected: Int = 0)

data class GenerationInfo(
  val state: GenerationWorkState = GenerationWorkState.IDLE,
  val progress: GenerationProgress = GenerationProgress(),
)

interface PuzzleGenerationScheduler {
  fun scheduleGeneration(modelId: ModelId)

  fun cancelGeneration()

  fun observeGenerationState(): Flow<GenerationWorkState>

  fun observeGenerationInfo(): Flow<GenerationInfo>
}
