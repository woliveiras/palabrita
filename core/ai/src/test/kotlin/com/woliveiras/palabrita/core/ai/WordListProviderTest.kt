package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.common.TextNormalizer
import org.junit.Test

class WordListProviderTest {

  private val provider = WordListProviderImpl()

  // --- Basic picking ---

  @Test
  fun `picks requested number of words`() {
    val words = provider.pickWords("en", 5, 10, emptySet())
    assertThat(words).hasSize(10)
  }

  @Test
  fun `picked words have correct normalized length`() {
    val words = provider.pickWords("pt", 6, 20, emptySet())
    val bad = words.filter { TextNormalizer.normalizeToAscii(it).length != 6 }
    assertThat(bad).isEmpty()
  }

  @Test
  fun `picks words from all supported languages`() {
    for (lang in listOf("en", "pt", "es")) {
      val words = provider.pickWords(lang, 5, 5, emptySet())
      assertThat(words).hasSize(5)
    }
  }

  // --- Exclusion ---

  @Test
  fun `excludes already existing words`() {
    val firstBatch = provider.pickWords("en", 5, 10, emptySet()).toSet()
    val secondBatch = provider.pickWords("en", 5, 10, firstBatch)

    val overlap = secondBatch.filter { TextNormalizer.normalizeToAscii(it) in firstBatch }
    assertThat(overlap).isEmpty()
  }

  @Test
  fun `returns fewer words when list is nearly exhausted`() {
    val allWords = WordList.getWords("en", 4).toSet()
    val allButFive = allWords.drop(5).toSet()
    val remaining = provider.pickWords("en", 4, 100, allButFive)
    assertThat(remaining.size).isAtMost(5)
  }

  @Test
  fun `returns empty list when all words are already used`() {
    val allWords = WordList.getWords("en", 4).toSet()
    val result = provider.pickWords("en", 4, 10, allWords)
    assertThat(result).isEmpty()
  }

  // --- Unknown language ---

  @Test
  fun `returns empty list for unknown language`() {
    val words = provider.pickWords("xx", 5, 10, emptySet())
    assertThat(words).isEmpty()
  }

  @Test
  fun `returns empty list for unsupported word length`() {
    val words = provider.pickWords("en", 99, 10, emptySet())
    assertThat(words).isEmpty()
  }

  // --- No duplicates in single pick ---

  @Test
  fun `picked words contain no duplicates`() {
    val words = provider.pickWords("es", 6, 50, emptySet())
    assertThat(words).containsNoDuplicates()
  }
}
