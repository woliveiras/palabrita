package com.woliveiras.palabrita.core.ai

import javax.inject.Inject

class PuzzleValidatorImpl @Inject constructor() : PuzzleValidator {

  private val validChars = Regex("^[a-z]+$")

  override fun validate(
    puzzle: PuzzleResponse,
    allExistingWords: Set<String>,
    expectedWordLength: IntRange,
  ): ValidationResult {
    val reasons = mutableListOf<String>()
    val word = puzzle.word.lowercase()

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

    // Hints count (accept 2-5; small models may produce fewer)
    if (puzzle.hints.size !in 2..5) {
      reasons.add("expected 2-5 hints, got ${puzzle.hints.size}")
    }

    // Hints must not contain the word
    puzzle.hints.forEachIndexed { index, hint ->
      if (hint.lowercase().contains(word)) {
        reasons.add("hint $index contains the word '$word'")
      }
    }

    // Category must not be blank
    if (puzzle.category.isBlank()) {
      reasons.add("category is blank")
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
