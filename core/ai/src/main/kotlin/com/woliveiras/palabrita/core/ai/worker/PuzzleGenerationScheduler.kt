package com.woliveiras.palabrita.core.ai.worker

import com.woliveiras.palabrita.core.model.ModelId

interface PuzzleGenerationScheduler {
  fun scheduleGeneration(modelId: ModelId)
}
