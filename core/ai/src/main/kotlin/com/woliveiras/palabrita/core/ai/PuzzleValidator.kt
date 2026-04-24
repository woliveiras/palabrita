package com.woliveiras.palabrita.core.ai

import com.woliveiras.palabrita.core.common.TextNormalizer
import com.woliveiras.palabrita.core.model.GameRules
import javax.inject.Inject

class PuzzleValidatorImpl @Inject constructor() : PuzzleValidator {

  private val validChars = Regex("^[a-z]+$")

  override fun validate(
    puzzle: PuzzleResponse,
    allExistingWords: Set<String>,
    expectedWordLength: IntRange,
  ): ValidationResult {
    val reasons = mutableListOf<String>()
    val word = TextNormalizer.normalizeToAscii(puzzle.word)

    // Character validation (before length check, since accents/spaces affect length perception)
    if (!validChars.matches(word)) {
      reasons.add("invalid characters in word '$word' — only [a-z] allowed")
    }

    // Word length
    if (word.length !in expectedWordLength) {
      reasons.add("word length ${word.length} not in range $expectedWordLength")
    }

    // Duplicate check
    if (word in allExistingWords) {
      reasons.add("duplicate word '$word'")
    }

    // Hints count — at least 3 required
    if (puzzle.hints.size < GameRules.MIN_HINTS) {
      reasons.add("expected at least ${GameRules.MIN_HINTS} hints, got ${puzzle.hints.size}")
    }

    // Hints must not contain the word
    puzzle.hints.forEachIndexed { index, hint ->
      if (hint.lowercase().contains(word)) {
        reasons.add("hint $index contains the word '$word'")
      }
    }

    return if (reasons.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(reasons)
  }
}

interface PuzzleValidator {
  fun validate(
    puzzle: PuzzleResponse,
    allExistingWords: Set<String>,
    expectedWordLength: IntRange,
  ): ValidationResult
}
