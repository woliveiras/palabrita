package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.ai.GeneratePuzzlesUseCase
import com.woliveiras.palabrita.core.ai.GenerationResult
import com.woliveiras.palabrita.core.model.ModelId

/**
 * Fake [GeneratePuzzlesUseCase] for use in unit tests.
 *
 * By default returns a successful result of 5 puzzles in a batch of 5. Set [result] before calling
 * [execute] to control the outcome.
 */
class FakeGeneratePuzzlesUseCase(
  var result: GenerationResult = GenerationResult(generatedCount = 5, batchSize = 5)
) : GeneratePuzzlesUseCase {
  var capturedLanguage: String? = null
  var capturedModelId: ModelId? = null
  var callCount: Int = 0

  override suspend fun execute(
    language: String,
    modelId: ModelId,
    onProgress: (successCount: Int, batchSize: Int) -> Unit,
  ): GenerationResult {
    callCount++
    capturedLanguage = language
    capturedModelId = modelId
    if (result.batchSize > 0 && result.generatedCount > 0) {
      onProgress(result.generatedCount, result.batchSize)
    }
    return result
  }
}
