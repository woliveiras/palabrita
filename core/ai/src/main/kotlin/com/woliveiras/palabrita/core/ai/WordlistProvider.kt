package com.woliveiras.palabrita.core.ai

import com.woliveiras.palabrita.core.common.TextNormalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Selects words from the curated [WordList] for puzzle generation. Words are picked
 * deterministically by shuffling with a seed derived from [allExistingWords] size, then filtering
 * out already-used words. This avoids the need for random state while still providing variety
 * across batches.
 */
interface WordListProvider {
  /**
   * Picks up to [count] words for the given [language] and [wordLength], excluding any word already
   * in [allExistingWords]. Returns fewer than [count] only if the word list is exhausted.
   */
  fun pickWords(
    language: String,
    wordLength: Int,
    count: Int,
    allExistingWords: Set<String>,
  ): List<String>
}

@Singleton
class WordListProviderImpl @Inject constructor() : WordListProvider {

  override fun pickWords(
    language: String,
    wordLength: Int,
    count: Int,
    allExistingWords: Set<String>,
  ): List<String> {
    val candidates =
      WordList.getWords(language, wordLength)
        .filter { word ->
          val normalized = TextNormalizer.normalizeToAscii(word)
          normalized.length == wordLength && normalized !in allExistingWords
        }
        .shuffled()

    return candidates.take(count)
  }
}
