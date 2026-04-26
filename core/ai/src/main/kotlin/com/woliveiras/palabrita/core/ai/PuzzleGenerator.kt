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
    hintLanguage: String = language,
    onPuzzleAttempted: suspend (successCount: Int) -> Unit = {},
  ): List<Puzzle>
}

/**
 * Hybrid puzzle generator: words come from a curated [WordListProvider], the LLM only generates
 * hints. This eliminates greedy-decoding issues (invented words, wrong length, loops) while keeping
 * the "AI-generated" feel for hints.
 *
 * If the LLM fails to produce valid hints after [GameRules.MAX_GENERATION_RETRIES] attempts, a
 * [HintFallbackProvider] supplies generic template-based hints so that puzzle generation never
 * fails.
 */
@Singleton
class PuzzleGeneratorImpl
@Inject
constructor(
  private val engineManager: LlmEngineManager,
  private val hintParser: LlmResponseParser,
  private val validator: PuzzleValidator,
  private val promptProvider: PromptProvider,
  private val wordListProvider: WordListProvider,
  private val hintFallback: HintFallbackProvider,
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
    hintLanguage: String,
    onPuzzleAttempted: suspend (successCount: Int) -> Unit,
  ): List<Puzzle> {
    require(engineManager.isReady()) { "Engine not ready" }

    val words = wordListProvider.pickWords(language, wordLength, count, allExistingWords)
    val generated = mutableListOf<Puzzle>()
    val usedWords = allExistingWords.toMutableSet()

    Log.i(
      TAG,
      "generateBatch: wordLength=$wordLength count=$count words=${words.size} hintLang=$hintLanguage",
    )

    val systemPrompt = promptProvider.hintSystemPrompt(hintLanguage)

    try {
      for ((index, word) in words.withIndex()) {
        _activity.value = GenerationActivity.CREATING
        val normalizedWord = TextNormalizer.normalizeToAscii(word)

        val hints = generateHintsForWord(systemPrompt, word, hintLanguage, normalizedWord)

        val puzzle =
          Puzzle(
            word = normalizedWord,
            wordDisplay = word.uppercase(),
            language = language,
            difficulty = wordLength,
            category = "",
            hints = hints,
            source = PuzzleSource.AI,
            generatedAt = System.currentTimeMillis(),
          )
        generated.add(puzzle)
        usedWords.add(normalizedWord)
        _activity.value = GenerationActivity.ACCEPTED
        Log.i(TAG, "  puzzle ${index + 1}/${words.size} OK: '$normalizedWord' hints=$hints")
        onPuzzleAttempted(generated.size)
      }
    } finally {
      _activity.value = null
    }

    Log.i(TAG, "generateBatch: done, ${generated.size}/${words.size} succeeded")
    return generated
  }

  /**
   * Asks the LLM to generate hints for a given word. Retries up to
   * [GameRules.MAX_GENERATION_RETRIES] times. If all attempts fail, returns fallback hints from
   * [HintFallbackProvider].
   */
  private suspend fun generateHintsForWord(
    systemPrompt: String,
    word: String,
    hintLanguage: String,
    normalizedWord: String,
  ): List<String> {
    repeat(GameRules.MAX_GENERATION_RETRIES) { attempt ->
      try {
        val userPrompt = promptProvider.hintUserPrompt(word, hintLanguage)
        val rawResponse = engineManager.generateSingleTurn(systemPrompt, userPrompt)
        Log.d(
          TAG,
          "  hint attempt $attempt response (${rawResponse.length} chars): ${rawResponse.take(200)}",
        )

        val parseResult = hintParser.parseHints(rawResponse)
        if (parseResult is ParseResult.Success) {
          val hints = parseResult.data
          // Validate hints don't contain the word
          val leaksWord = hints.any { hint -> hint.lowercase().contains(normalizedWord) }
          if (!leaksWord && hints.size >= GameRules.MIN_HINTS) {
            Log.d(TAG, "  hint attempt $attempt VALID")
            return hints.take(GameRules.MIN_HINTS)
          }
          Log.w(TAG, "  hint attempt $attempt invalid: leaksWord=$leaksWord size=${hints.size}")
        } else {
          Log.w(
            TAG,
            "  hint attempt $attempt parse failed: ${(parseResult as ParseResult.Error).reason}",
          )
        }
      } catch (e: Exception) {
        Log.w(TAG, "  hint attempt $attempt exception: ${e.message}")
      }
    }

    Log.w(TAG, "  all hint attempts failed for '$word', using fallback")
    return hintFallback.fallbackHints(word, hintLanguage)
  }

  companion object {
    private const val TAG = "PuzzleGenerator"
  }
}
