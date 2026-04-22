package com.woliveiras.palabrita.core.ai

import android.util.Log
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.PuzzleSource
import javax.inject.Inject
import javax.inject.Singleton

interface PuzzleGenerator {
  suspend fun generateBatch(
    count: Int,
    language: String,
    targetDifficulty: Int,
    recentWords: List<String>,
    allExistingWords: Set<String>,
    modelId: ModelId,
    onPuzzleAttempted: suspend (successCount: Int) -> Unit = {},
  ): List<Puzzle>
}

@Singleton
class PuzzleGeneratorImpl
@Inject
constructor(
  private val engineManager: LlmEngineManager,
  private val parser: LlmResponseParser,
  private val validator: PuzzleValidator,
) : PuzzleGenerator {

  override suspend fun generateBatch(
    count: Int,
    language: String,
    targetDifficulty: Int,
    recentWords: List<String>,
    allExistingWords: Set<String>,
    modelId: ModelId,
    onPuzzleAttempted: suspend (successCount: Int) -> Unit,
  ): List<Puzzle> {
    require(engineManager.isReady()) { "Engine not ready" }

    val wordLength = difficultyToWordLength(targetDifficulty)
    val generated = mutableListOf<Puzzle>()
    val usedWords = allExistingWords.toMutableSet()

    Log.i(TAG, "generateBatch: difficulty=$targetDifficulty count=$count length=$wordLength")

    val batchWords = mutableListOf<String>()

    for (i in 0 until count) {
      val puzzle =
        generateSinglePuzzle(
          language = language,
          difficulty = targetDifficulty,
          wordLength = wordLength,
          recentWords = recentWords + batchWords,
          usedWords = usedWords,
          modelId = modelId,
        )
      if (puzzle != null) {
        generated.add(puzzle)
        usedWords.add(puzzle.word)
        batchWords.add(puzzle.word)
        Log.i(TAG, "  puzzle ${i + 1}/$count OK: '${puzzle.word}'")
      } else {
        Log.w(TAG, "  puzzle ${i + 1}/$count FAILED after $MAX_RETRIES retries")
      }
      onPuzzleAttempted(generated.size)
    }

    Log.i(TAG, "generateBatch: done, ${generated.size}/$count succeeded")
    return generated
  }

  private suspend fun generateSinglePuzzle(
    language: String,
    difficulty: Int,
    wordLength: IntRange,
    recentWords: List<String>,
    usedWords: Set<String>,
    modelId: ModelId,
  ): Puzzle? {
    repeat(MAX_RETRIES) { attempt ->
      val rawResponse = callLlm(language, difficulty, wordLength, recentWords, modelId, attempt)
      Log.d(
        TAG,
        "  attempt $attempt response (${rawResponse.length} chars): ${rawResponse.take(300)}",
      )
      val parseResult = parser.parsePuzzle(rawResponse)

      when (parseResult) {
        is ParseResult.Success -> {
          val validation = validator.validate(parseResult.data, usedWords, wordLength)
          when (validation) {
            is ValidationResult.Valid -> {
              Log.d(TAG, "  attempt $attempt VALID: '${parseResult.data.word}'")
              return puzzleFromResponse(parseResult.data, language, difficulty)
            }
            is ValidationResult.Invalid -> {
              Log.w(TAG, "  attempt $attempt validation failed: ${validation.reasons}")
            }
          }
        }
        is ParseResult.Error -> {
          Log.w(TAG, "  attempt $attempt parse failed: ${parseResult.reason}")
        }
      }
    }
    return null
  }

  private suspend fun callLlm(
    language: String,
    difficulty: Int,
    wordLength: IntRange,
    recentWords: List<String>,
    modelId: ModelId,
    attempt: Int,
  ): String {
    val systemPrompt: String?
    val userPrompt: String

    when (modelId) {
      ModelId.GEMMA4_E2B -> {
        systemPrompt = PromptTemplates.puzzleSystemPromptGemma4()
        val basePrompt =
          PromptTemplates.puzzleUserPromptGemma4(
            language,
            difficulty,
            wordLength.first,
            wordLength.last,
            if (attempt < 2) recentWords else emptyList(),
          )
        userPrompt =
          when (attempt) {
            0 -> basePrompt
            1 ->
              "$basePrompt\n\nThe previous response was invalid. " +
                "Please try again with a different word."
            else ->
              PromptTemplates.puzzleUserPromptGemma4(
                language,
                difficulty,
                wordLength.first,
                wordLength.last,
                emptyList(),
              )
          }
      }
      ModelId.QWEN3_0_6B -> {
        systemPrompt = null
        val basePrompt =
          PromptTemplates.puzzlePromptGemma3(
            language,
            difficulty,
            wordLength.first,
            wordLength.last,
            if (attempt < 2) recentWords else emptyList(),
          )
        userPrompt =
          when (attempt) {
            0 -> basePrompt
            1 ->
              "$basePrompt\n\nThe previous response was invalid. " +
                "Please try again with a different word."
            else ->
              PromptTemplates.puzzlePromptGemma3(
                language,
                difficulty,
                wordLength.first,
                wordLength.last,
                emptyList(),
              )
          }
      }
      ModelId.NONE -> throw IllegalArgumentException("No model selected")
    }

    return engineManager.generateSingleTurn(systemPrompt, userPrompt)
  }

  private fun puzzleFromResponse(
    response: PuzzleResponse,
    language: String,
    difficulty: Int,
  ): Puzzle =
    Puzzle(
      word = response.word.lowercase(),
      wordDisplay = response.word.uppercase(),
      language = language,
      difficulty = difficulty,
      category = response.category,
      hints = response.hints,
      source = PuzzleSource.AI,
      generatedAt = System.currentTimeMillis(),
    )

  companion object {
    private const val TAG = "PuzzleGenerator"
    private const val MAX_RETRIES = 3

    fun difficultyToWordLength(difficulty: Int): IntRange =
      when (difficulty) {
        1 -> 4..5
        2 -> 5..6
        3 -> 5..7
        4 -> 6..8
        5 -> 7..9
        else -> 5..6
      }
  }
}
