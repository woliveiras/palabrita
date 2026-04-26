package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.ai.GenerationActivity
import com.woliveiras.palabrita.core.ai.PuzzleGenerator
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.PuzzleSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake [PuzzleGenerator] for unit tests.
 *
 * By default returns an empty list. Use [setBatchResults] to configure how many puzzles each
 * successive [generateBatch] call should return, expressed as a fraction of the requested [count].
 * For example, `setBatchResults(0.8f, 1.0f)` means the first call returns 80% of requested slots
 * and the second call returns 100%.
 *
 * Once the configured results are exhausted, subsequent calls return the full [count].
 */
class FakePuzzleGenerator : PuzzleGenerator {
  private val _activity = MutableStateFlow<GenerationActivity?>(null)
  override val activity: StateFlow<GenerationActivity?> = _activity

  var callCount = 0
    private set

  private val batchResults = mutableListOf<Float>()

  /** Each value is the fraction of [count] to return on successive [generateBatch] calls. */
  fun setBatchResults(vararg fractions: Float) {
    batchResults.clear()
    batchResults.addAll(fractions.toList())
  }

  override suspend fun generateBatch(
    count: Int,
    language: String,
    wordLength: Int,
    recentWords: List<String>,
    allExistingWords: Set<String>,
    modelId: ModelId,
    hintLanguage: String,
    onPuzzleAttempted: suspend (successCount: Int) -> Unit,
  ): List<Puzzle> {
    val fraction = batchResults.getOrNull(callCount) ?: 0f
    callCount++
    val toGenerate = (count * fraction).toInt().coerceIn(0, count)
    val puzzles =
      (1..toGenerate).map { i ->
        Puzzle(
          id = 0,
          word = "word$callCount$i",
          wordDisplay = "WORD$callCount$i",
          language = language,
          difficulty = wordLength,
          category = "",
          hints = listOf("hint 1", "hint 2", "hint 3"),
          source = PuzzleSource.AI,
          generatedAt = System.currentTimeMillis(),
        )
      }
    puzzles.forEachIndexed { index, _ -> onPuzzleAttempted(index + 1) }
    return puzzles
  }
}
