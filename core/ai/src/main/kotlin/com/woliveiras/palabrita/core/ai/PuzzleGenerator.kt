package com.woliveiras.palabrita.core.ai

import android.util.Log
import com.woliveiras.palabrita.core.common.TextNormalizer
import com.woliveiras.palabrita.core.model.GameRules
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.PuzzleSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface PuzzleGenerator {
  val activity: StateFlow<GenerationActivity?>

  suspend fun generateBatch(
    count: Int,
    language: String,
    wordLength: Int,
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
  private val promptProvider: PromptProvider,
) : PuzzleGenerator {

  private val _activity = MutableStateFlow<GenerationActivity?>(null)
  override val activity: StateFlow<GenerationActivity?> = _activity

  override suspend fun generateBatch(
    count: Int,
    language: String,
    wordLength: Int,
    recentWords: List<String>,
    allExistingWords: Set<String>,
    modelId: ModelId,
    onPuzzleAttempted: suspend (successCount: Int) -> Unit,
  ): List<Puzzle> {
    require(engineManager.isReady()) { "Engine not ready" }

    val lengthRange = wordLength..wordLength
    val generated = mutableListOf<Puzzle>()
    val usedWords = allExistingWords.toMutableSet()

    Log.i(TAG, "generateBatch: wordLength=$wordLength count=$count")

    val systemPrompt = promptProvider.puzzleSystemPrompt()
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
              wordLength = wordLength,
              lengthRange = lengthRange,
              recentWords = recentWords,
              usedWords = usedWords,
              modelId = modelId,
            )
          if (puzzle != null) {
            generated.add(puzzle)
            usedWords.add(puzzle.word)
            Log.i(TAG, "  puzzle ${i + 1}/$count OK: '${puzzle.word}'")
          } else {
            Log.w(
              TAG,
              "  puzzle ${i + 1}/$count FAILED after ${GameRules.MAX_GENERATION_RETRIES} retries",
            )
          }
          onPuzzleAttempted(generated.size)
        }
      }
      Log.d(TAG, "  session rotated after $chunkSize puzzles (${generated.size} total OK)")
    }

    Log.i(TAG, "generateBatch: done, ${generated.size}/$count succeeded")
    _activity.value = null
    return generated
  }

  private suspend fun generateSinglePuzzle(
    session: LlmSession,
    language: String,
    wordLength: Int,
    lengthRange: IntRange,
    recentWords: List<String>,
    usedWords: Set<String>,
    modelId: ModelId,
  ): Puzzle? {
    var lastFailureReason: String? = null
    repeat(GameRules.MAX_GENERATION_RETRIES) { attempt ->
      _activity.value = GenerationActivity.CREATING
      val userPrompt =
        buildUserPrompt(
          modelId,
          language,
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
      _activity.value = GenerationActivity.VALIDATING
      val parseResult = parser.parsePuzzle(rawResponse)

      when (parseResult) {
        is ParseResult.Success -> {
          val validation = validator.validate(parseResult.data, usedWords, lengthRange)
          when (validation) {
            is ValidationResult.Valid -> {
              Log.d(TAG, "  attempt $attempt VALID: '${parseResult.data.word}'")
              _activity.value = GenerationActivity.ACCEPTED
              return puzzleFromResponse(parseResult.data, language, wordLength)
            }
            is ValidationResult.Invalid -> {
              lastFailureReason = validation.reasons.joinToString("; ")
              Log.w(TAG, "  attempt $attempt validation failed: ${validation.reasons}")
              _activity.value = GenerationActivity.VALIDATION_FAILED
            }
          }
        }
        is ParseResult.Error -> {
          lastFailureReason = "JSON parse error: ${parseResult.reason}"
          Log.w(TAG, "  attempt $attempt parse failed: ${parseResult.reason}")
          _activity.value = GenerationActivity.VALIDATION_FAILED
        }
      }
    }
    _activity.value = GenerationActivity.FAILED_RETRYING
    return null
  }

  private fun buildUserPrompt(
    modelId: ModelId,
    language: String,
    wordLength: Int,
    recentWords: List<String>,
    usedWords: Set<String>,
    attempt: Int,
    failureReason: String? = null,
  ): String {
    val avoidWords = (recentWords + usedWords).distinct().takeLast(30)
    val base =
      when (modelId) {
        ModelId.GEMMA4_E4B,
        ModelId.GEMMA4_E2B -> promptProvider.puzzleUserPromptLarge(language, wordLength, avoidWords)
        ModelId.PHI4_MINI,
        ModelId.DEEPSEEK_R1_1_5B,
        ModelId.QWEN2_5_1_5B,
        ModelId.QWEN3_0_6B -> promptProvider.puzzlePromptCompact(language, wordLength, avoidWords)
        ModelId.NONE -> throw IllegalArgumentException("No model selected")
      }
    return if (attempt > 0 && failureReason != null) {
      "$base\n\nYour previous response was REJECTED for this reason: $failureReason. Fix exactly that issue and choose a DIFFERENT word."
    } else if (attempt > 0) {
      "$base\n\nThe previous response was invalid. Generate a DIFFERENT word."
    } else {
      base
    }
  }

  private fun puzzleFromResponse(
    response: PuzzleResponse,
    language: String,
    wordLength: Int,
  ): Puzzle =
    Puzzle(
      word = TextNormalizer.normalizeToAscii(response.word),
      wordDisplay = response.word.uppercase(),
      language = language,
      difficulty = wordLength,
      category = "",
      hints = response.hints.take(GameRules.MIN_HINTS),
      source = PuzzleSource.AI,
      generatedAt = System.currentTimeMillis(),
    )

  companion object {
    private const val TAG = "PuzzleGenerator"
    // Rotate LLM sessions every N puzzles to keep context manageable.
    // Must not leave a remainder of 1 for any expected batch size (5, 10).
    // SESSION_ROTATION=5 → batchSize=5: [5], batchSize=10: [5,5]. No isolated single-puzzle sessions.
    private const val SESSION_ROTATION = 5
  }
}
