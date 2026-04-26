package com.woliveiras.palabrita.core.ai

import android.util.Log
import com.woliveiras.palabrita.core.model.GameRules
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

data class GenerationResult(val generatedCount: Int, val batchSize: Int)

interface GeneratePuzzlesUseCase {
  /**
   * Generates a batch of puzzles for the given [language] and [modelId] and persists them.
   *
   * @param language ISO language code (e.g. "pt", "en", "es")
   * @param modelId The model to use for generation
   * @param onProgress Called after each successfully generated puzzle with (successCount,
   *   batchSize)
   * @return [GenerationResult] with the final counts; [GenerationResult.generatedCount] may be 0 if
   *   all LLM retries failed, and [GenerationResult.batchSize] is -1 if generation was skipped
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
  private val gameSessionRepository: GameSessionRepository,
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
    val hintLanguage = appPreferences.appLanguage.first()

    val cycle = appPreferences.generationCycle.first()
    val level = GameRules.levelForCycle(cycle)
    val wins = gameSessionRepository.countWinsByDifficulty(level.wordLength, language)

    // Mastery gate: decide whether to advance or stay at current level
    val (wordLength, batchSize) =
      if (wins >= level.winsRequired) {
        appPreferences.incrementGenerationCycle()
        val nextLevel = GameRules.levelForCycle(cycle + 1)
        Log.i(TAG, "Mastery met ($wins/${level.winsRequired} wins), advancing to ${nextLevel.wordLength}-letter words")
        nextLevel.wordLength to nextLevel.batchSize
      } else {
        val remaining = (level.winsRequired - wins).coerceAtMost(level.batchSize)
        Log.i(TAG, "Mastery not met ($wins/${level.winsRequired} wins), generating $remaining more ${level.wordLength}-letter words")
        level.wordLength to remaining
      }

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
          hintLanguage = hintLanguage,
        ) { successCount ->
          onProgress(successCount, batchSize)
        }
      puzzleRepository.savePuzzles(puzzles)
      generatedCount = puzzles.size

      // Retry loop: keep filling missing slots across multiple passes.
      // Use mutable collections to avoid repeated allocation on each pass.
      val updatedExistingWords = (existingWords + puzzles.map { it.word }).toMutableSet()
      val updatedRecentWords = (recentWords + puzzles.map { it.word }).toMutableList()
      var retryPass = 0

      while (generatedCount < batchSize && retryPass < MAX_BATCH_RETRY_PASSES) {
        retryPass++
        val missing = batchSize - generatedCount
        Log.w(
          TAG,
          "Retry pass $retryPass/$MAX_BATCH_RETRY_PASSES: $generatedCount/$batchSize, filling $missing slot(s)",
        )
        val countBeforeRetry = generatedCount
        val retryPuzzles =
          puzzleGenerator.generateBatch(
            count = missing,
            language = language,
            wordLength = wordLength,
            recentWords = updatedRecentWords,
            allExistingWords = updatedExistingWords,
            modelId = modelId,
            hintLanguage = hintLanguage,
          ) { successCount ->
            onProgress(countBeforeRetry + successCount, batchSize)
          }
        puzzleRepository.savePuzzles(retryPuzzles)
        generatedCount += retryPuzzles.size
        updatedExistingWords.addAll(retryPuzzles.map { it.word })
        updatedRecentWords.addAll(retryPuzzles.map { it.word })
      }

      if (generatedCount < batchSize) {
        Log.w(TAG, "Gave up after $retryPass retry pass(es): $generatedCount/$batchSize generated")
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Batch generation failed", e)
    }

    return GenerationResult(generatedCount = generatedCount, batchSize = batchSize)
  }

  private companion object {
    const val TAG = "GeneratePuzzlesUseCase"
    /** Maximum additional passes to fill slots the LLM failed on the first attempt. */
    const val MAX_BATCH_RETRY_PASSES = 3
  }
}
