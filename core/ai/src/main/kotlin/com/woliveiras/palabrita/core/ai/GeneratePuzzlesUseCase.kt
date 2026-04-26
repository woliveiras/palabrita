package com.woliveiras.palabrita.core.ai

import android.util.Log
import com.woliveiras.palabrita.core.model.GameRules
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class GenerationResult(
  val generatedCount: Int,
  val batchSize: Int,
)

interface GeneratePuzzlesUseCase {
  /**
   * Generates a batch of puzzles for the given [language] and [modelId] and persists them.
   *
   * @param language ISO language code (e.g. "pt", "en", "es")
   * @param modelId The model to use for generation
   * @param onProgress Called after each successfully generated puzzle with (successCount, batchSize)
   * @return [GenerationResult] with the final counts; [GenerationResult.generatedCount] may be 0
   *   if all LLM retries failed, and [GenerationResult.batchSize] is -1 if generation was skipped
   *   because the threshold was already met.
   */
  suspend fun execute(
    language: String,
    modelId: ModelId,
    onProgress: (successCount: Int, batchSize: Int) -> Unit = { _, _ -> },
  ): GenerationResult
}

@Singleton
class GeneratePuzzlesUseCaseImpl
@Inject
constructor(
  private val puzzleRepository: PuzzleRepository,
  private val puzzleGenerator: PuzzleGenerator,
  private val engineManager: LlmEngineManager,
  private val appPreferences: AppPreferences,
) : GeneratePuzzlesUseCase {

  override suspend fun execute(
    language: String,
    modelId: ModelId,
    onProgress: (successCount: Int, batchSize: Int) -> Unit,
  ): GenerationResult {
    val unplayedCount = puzzleRepository.countAllUnplayed(language)
    if (unplayedCount >= GameRules.REPLENISHMENT_THRESHOLD) {
      return GenerationResult(generatedCount = 0, batchSize = -1)
    }

    require(engineManager.isReady()) { "Engine not ready" }

    val existingWords = puzzleRepository.getAllGeneratedWords()
    val recentWords = puzzleRepository.getRecentWords(50)

    val cycle = appPreferences.generationCycle.first()
    val (wordLength, batchSize) = GameRules.levelForCycle(cycle)

    var generatedCount = 0
    try {
      val puzzles =
        puzzleGenerator.generateBatch(
          count = batchSize,
          language = language,
          wordLength = wordLength,
          recentWords = recentWords,
          allExistingWords = existingWords,
          modelId = modelId,
        ) { successCount ->
          onProgress(successCount, batchSize)
        }
      puzzleRepository.savePuzzles(puzzles)
      generatedCount = puzzles.size

      val missing = batchSize - generatedCount
      if (missing > 0) {
        Log.w(TAG, "$generatedCount/$batchSize puzzles generated; retrying $missing missing slot(s)")
        val retryExistingWords = existingWords + puzzles.map { it.word }.toSet()
        val retryRecentWords = recentWords + puzzles.map { it.word }
        val retryPuzzles =
          puzzleGenerator.generateBatch(
            count = missing,
            language = language,
            wordLength = wordLength,
            recentWords = retryRecentWords,
            allExistingWords = retryExistingWords,
            modelId = modelId,
          )
        puzzleRepository.savePuzzles(retryPuzzles)
        generatedCount += retryPuzzles.size
      }

      if (generatedCount > 0) {
        appPreferences.incrementGenerationCycle()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Batch generation failed", e)
    }

    return GenerationResult(generatedCount = generatedCount, batchSize = batchSize)
  }

  private companion object {
    const val TAG = "GeneratePuzzlesUseCase"
  }
}
