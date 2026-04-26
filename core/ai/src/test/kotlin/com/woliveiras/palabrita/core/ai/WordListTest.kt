package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.woliveiras.palabrita.core.common.TextNormalizer
import org.junit.Test

class WordListTest {

  // --- Loading ---

  @Test
  fun `loads English word list successfully`() {
    val words4 = WordList.getWords("en", 4)
    val words5 = WordList.getWords("en", 5)
    val words6 = WordList.getWords("en", 6)

    assertThat(words4).isNotEmpty()
    assertThat(words5).isNotEmpty()
    assertThat(words6).isNotEmpty()
  }

  @Test
  fun `loads Portuguese word list successfully`() {
    val words4 = WordList.getWords("pt", 4)
    val words5 = WordList.getWords("pt", 5)
    val words6 = WordList.getWords("pt", 6)

    assertThat(words4).isNotEmpty()
    assertThat(words5).isNotEmpty()
    assertThat(words6).isNotEmpty()
  }

  @Test
  fun `loads Spanish word list successfully`() {
    val words4 = WordList.getWords("es", 4)
    val words5 = WordList.getWords("es", 5)
    val words6 = WordList.getWords("es", 6)

    assertThat(words4).isNotEmpty()
    assertThat(words5).isNotEmpty()
    assertThat(words6).isNotEmpty()
  }

  @Test
  fun `returns empty list for unknown language`() {
    val words = WordList.getWords("xx", 5)
    assertThat(words).isEmpty()
  }

  @Test
  fun `returns empty list for unsupported word length`() {
    val words = WordList.getWords("en", 99)
    assertThat(words).isEmpty()
  }

  // --- Word length integrity ---

  @Test
  fun `all English 4-letter words have normalized length 4`() {
    val words = WordList.getWords("en", 4)
    val bad = words.filter { TextNormalizer.normalizeToAscii(it).length != 4 }
    assertThat(bad).isEmpty()
  }

  @Test
  fun `all English 5-letter words have normalized length 5`() {
    val words = WordList.getWords("en", 5)
    val bad = words.filter { TextNormalizer.normalizeToAscii(it).length != 5 }
    assertThat(bad).isEmpty()
  }

  @Test
  fun `all English 6-letter words have normalized length 6`() {
    val words = WordList.getWords("en", 6)
    val bad = words.filter { TextNormalizer.normalizeToAscii(it).length != 6 }
    assertThat(bad).isEmpty()
  }

  @Test
  fun `all Portuguese 4-letter words have normalized length 4`() {
    val words = WordList.getWords("pt", 4)
    val bad = words.filter { TextNormalizer.normalizeToAscii(it).length != 4 }
    assertThat(bad).isEmpty()
  }

  @Test
  fun `all Portuguese 5-letter words have normalized length 5`() {
    val words = WordList.getWords("pt", 5)
    val bad = words.filter { TextNormalizer.normalizeToAscii(it).length != 5 }
    assertThat(bad).isEmpty()
  }

  @Test
  fun `all Portuguese 6-letter words have normalized length 6`() {
    val words = WordList.getWords("pt", 6)
    val bad = words.filter { TextNormalizer.normalizeToAscii(it).length != 6 }
    assertThat(bad).isEmpty()
  }

  @Test
  fun `all Spanish 4-letter words have normalized length 4`() {
    val words = WordList.getWords("es", 4)
    val bad = words.filter { TextNormalizer.normalizeToAscii(it).length != 4 }
    assertThat(bad).isEmpty()
  }

  @Test
  fun `all Spanish 5-letter words have normalized length 5`() {
    val words = WordList.getWords("es", 5)
    val bad = words.filter { TextNormalizer.normalizeToAscii(it).length != 5 }
    assertThat(bad).isEmpty()
  }

  @Test
  fun `all Spanish 6-letter words have normalized length 6`() {
    val words = WordList.getWords("es", 6)
    val bad = words.filter { TextNormalizer.normalizeToAscii(it).length != 6 }
    assertThat(bad).isEmpty()
  }

  // --- No duplicates ---

  @Test
  fun `English word lists have no duplicates`() {
    for (len in listOf(4, 5, 6)) {
      val words = WordList.getWords("en", len)
      assertThat(words).containsNoDuplicates()
    }
  }

  @Test
  fun `Portuguese word lists have no duplicates`() {
    for (len in listOf(4, 5, 6)) {
      val words = WordList.getWords("pt", len)
      assertThat(words).containsNoDuplicates()
    }
  }

  @Test
  fun `Spanish word lists have no duplicates`() {
    for (len in listOf(4, 5, 6)) {
      val words = WordList.getWords("es", len)
      assertThat(words).containsNoDuplicates()
    }
  }

  // --- Minimum quantity for gameplay ---

  @Test
  fun `each language has at least 100 words per length`() {
    for (lang in listOf("en", "pt", "es")) {
      for (len in listOf(4, 5, 6)) {
        val words = WordList.getWords(lang, len)
        assertWithMessage("$lang $len-letter words")
          .that(words.size)
          .isAtLeast(100)
      }
    }
  }

  @Test
  fun `6-letter lists have at least 200 words for long gameplay`() {
    for (lang in listOf("en", "pt", "es")) {
      val words = WordList.getWords(lang, 6)
      assertWithMessage("$lang 6-letter words")
        .that(words.size)
        .isAtLeast(200)
    }
  }

  // --- All lowercase ---

  @Test
  fun `all words are lowercase`() {
    for (lang in listOf("en", "pt", "es")) {
      for (len in listOf(4, 5, 6)) {
        val words = WordList.getWords(lang, len)
        val nonLower = words.filter { it != it.lowercase() }
        assertWithMessage("$lang $len-letter non-lowercase words")
          .that(nonLower)
          .isEmpty()
      }
    }
  }

  // --- Only alphabetic ---

  @Test
  fun `all words contain only letters`() {
    for (lang in listOf("en", "pt", "es")) {
      for (len in listOf(4, 5, 6)) {
        val words = WordList.getWords(lang, len)
        val nonAlpha = words.filter { word -> !word.all { it.isLetter() } }
        assertWithMessage("$lang $len-letter non-alpha words")
          .that(nonAlpha)
          .isEmpty()
      }
    }
  }
}
