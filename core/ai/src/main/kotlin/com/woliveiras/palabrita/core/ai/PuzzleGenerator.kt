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

    val systemPrompt = buildSystemPrompt(modelId)
    var puzzleIndex = 0

    while (puzzleIndex < count) {
      val remaining = count - puzzleIndex
      val chunkSize = remaining.coerceAtMost(SESSION_ROTATION)

      engineManager.createChatSession(systemPrompt).use { session ->
        repeat(chunkSize) {
          val i = puzzleIndex++
          val puzzle =
            generateSinglePuzzle(
              session = session,
              language = language,
              difficulty = targetDifficulty,
              wordLength = wordLength,
              recentWords = recentWords,
              usedWords = usedWords,
              modelId = modelId,
            )
          if (puzzle != null) {
            generated.add(puzzle)
            usedWords.add(puzzle.word)
            Log.i(TAG, "  puzzle ${i + 1}/$count OK: '${puzzle.word}'")
          } else {
            Log.w(TAG, "  puzzle ${i + 1}/$count FAILED after $MAX_RETRIES retries")
          }
          onPuzzleAttempted(generated.size)
        }
      }
      Log.d(TAG, "  session rotated after $chunkSize puzzles (${generated.size} total OK)")
    }

    Log.i(TAG, "generateBatch: done, ${generated.size}/$count succeeded")
    return generated
  }

  private suspend fun generateSinglePuzzle(
    session: LlmSession,
    language: String,
    difficulty: Int,
    wordLength: IntRange,
    recentWords: List<String>,
    usedWords: Set<String>,
    modelId: ModelId,
  ): Puzzle? {
    var lastFailureReason: String? = null
    repeat(MAX_RETRIES) { attempt ->
      val userPrompt =
        buildUserPrompt(
          modelId,
          language,
          difficulty,
          wordLength,
          recentWords,
          usedWords,
          attempt,
          lastFailureReason,
        )
      val rawResponse = session.sendMessage(userPrompt)
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
              lastFailureReason = validation.reasons.joinToString("; ")
              Log.w(TAG, "  attempt $attempt validation failed: ${validation.reasons}")
            }
          }
        }
        is ParseResult.Error -> {
          lastFailureReason = "JSON parse error: ${parseResult.reason}"
          Log.w(TAG, "  attempt $attempt parse failed: ${parseResult.reason}")
        }
      }
    }
    return null
  }

  private fun buildSystemPrompt(modelId: ModelId): String =
    when (modelId) {
      ModelId.GEMMA4_E4B,
      ModelId.GEMMA4_E2B -> PromptTemplates.puzzleSystemPromptGemma4()
      ModelId.PHI4_MINI,
      ModelId.DEEPSEEK_R1_1_5B,
      ModelId.QWEN2_5_1_5B,
      ModelId.QWEN3_0_6B,
      ModelId.NONE -> PromptTemplates.puzzleSystemPromptGemma4()
    }

  private fun buildUserPrompt(
    modelId: ModelId,
    language: String,
    difficulty: Int,
    wordLength: IntRange,
    recentWords: List<String>,
    usedWords: Set<String>,
    attempt: Int,
    failureReason: String? = null,
  ): String {
    val avoidWords = (recentWords + usedWords).distinct().takeLast(30)
    val base =
      when (modelId) {
        ModelId.GEMMA4_E4B,
        ModelId.GEMMA4_E2B ->
          PromptTemplates.puzzleUserPromptGemma4(
            language,
            difficulty,
            wordLength.first,
            wordLength.last,
            avoidWords,
          )
        ModelId.PHI4_MINI,
        ModelId.DEEPSEEK_R1_1_5B,
        ModelId.QWEN2_5_1_5B,
        ModelId.QWEN3_0_6B ->
          PromptTemplates.puzzlePromptGemma3(
            language,
            difficulty,
            wordLength.first,
            wordLength.last,
            avoidWords,
          )
        ModelId.NONE -> throw IllegalArgumentException("No model selected")
      }
    return if (attempt > 0 && failureReason != null) {
      "$base\n\nYour previous response was rejected: $failureReason. The word MUST have ${wordLength.first}-${wordLength.last} letters. Try again with a DIFFERENT word."
    } else if (attempt > 0) {
      "$base\n\nThe previous response was invalid. Generate a DIFFERENT word with ${wordLength.first}-${wordLength.last} letters."
    } else {
      base
    }
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
    private const val MAX_RETRIES = 5
    private const val SESSION_ROTATION = 3

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
