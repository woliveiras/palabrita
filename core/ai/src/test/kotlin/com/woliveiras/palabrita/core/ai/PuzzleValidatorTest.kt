package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PuzzleValidatorTest {

  private val validator = PuzzleValidatorImpl()

  // --- Word length ---

  @Test
  fun `valid word within expected range is accepted`() {
    val puzzle = createTestPuzzleResponse(word = "gatos") // 5 chars
    val result = validator.validate(puzzle, emptySet(), 5..5)
    assertThat(result).isEqualTo(ValidationResult.Valid)
  }

  @Test
  fun `word shorter than min length is rejected`() {
    val puzzle = createTestPuzzleResponse(word = "gato") // 4 chars
    val result = validator.validate(puzzle, emptySet(), 5..6)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
    assertThat((result as ValidationResult.Invalid).reasons).contains("word length 4 not in range 5..6")
  }

  @Test
  fun `word longer than max length is rejected`() {
    val puzzle = createTestPuzzleResponse(word = "hipopotamo") // 10 chars
    val result = validator.validate(puzzle, emptySet(), 5..8)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
  }

  @Test
  fun `word at exact min length is accepted`() {
    val puzzle = createTestPuzzleResponse(word = "gatos") // 5 chars
    val result = validator.validate(puzzle, emptySet(), 5..8)
    assertThat(result).isEqualTo(ValidationResult.Valid)
  }

  @Test
  fun `word at exact max length is accepted`() {
    val puzzle = createTestPuzzleResponse(word = "elefante") // 8 chars
    val result = validator.validate(puzzle, emptySet(), 5..8)
    assertThat(result).isEqualTo(ValidationResult.Valid)
  }

  // --- Character validation ---

  @Test
  fun `word with accents is rejected`() {
    val puzzle = createTestPuzzleResponse(word = "café")
    val result = validator.validate(puzzle, emptySet(), 4..6)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
    assertThat((result as ValidationResult.Invalid).reasons.any { "invalid characters" in it }).isTrue()
  }

  @Test
  fun `word with spaces is rejected`() {
    val puzzle = createTestPuzzleResponse(word = "bom dia")
    val result = validator.validate(puzzle, emptySet(), 5..8)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
  }

  @Test
  fun `word with hyphens is rejected`() {
    val puzzle = createTestPuzzleResponse(word = "bem-te-vi")
    val result = validator.validate(puzzle, emptySet(), 5..10)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
  }

  @Test
  fun `word with uppercase is normalized and accepted`() {
    val puzzle = createTestPuzzleResponse(word = "GATOS")
    val result = validator.validate(puzzle, emptySet(), 5..5)
    assertThat(result).isEqualTo(ValidationResult.Valid)
  }

  @Test
  fun `word with numbers is rejected`() {
    val puzzle = createTestPuzzleResponse(word = "abc123")
    val result = validator.validate(puzzle, emptySet(), 5..8)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
  }

  // --- Duplicate check ---

  @Test
  fun `duplicate word is rejected`() {
    val existing = setOf("gatos", "campo", "mundo")
    val puzzle = createTestPuzzleResponse(word = "gatos")
    val result = validator.validate(puzzle, existing, 5..5)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
    assertThat((result as ValidationResult.Invalid).reasons.any { "duplicate" in it }).isTrue()
  }

  @Test
  fun `non-duplicate word is accepted`() {
    val existing = setOf("campo", "mundo")
    val puzzle = createTestPuzzleResponse(word = "gatos")
    val result = validator.validate(puzzle, existing, 5..5)
    assertThat(result).isEqualTo(ValidationResult.Valid)
  }

  // --- Hints validation ---

  @Test
  fun `fewer than 5 hints is rejected`() {
    val puzzle = createTestPuzzleResponse(hints = listOf("Dica 1", "Dica 2", "Dica 3"))
    val result = validator.validate(puzzle, emptySet(), 5..5)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
    assertThat((result as ValidationResult.Invalid).reasons.any { "5 hints" in it }).isTrue()
  }

  @Test
  fun `more than 5 hints is rejected`() {
    val puzzle = createTestPuzzleResponse(hints = List(6) { "Dica ${it + 1}" })
    val result = validator.validate(puzzle, emptySet(), 5..5)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
  }

  @Test
  fun `hint containing the word is rejected`() {
    val puzzle = createTestPuzzleResponse(word = "gatos", hints = listOf(
      "Tem quatro patas",
      "É um animal doméstico",
      "Os gatos ronronam",
      "Persegue ratos",
      "Mia",
    ))
    val result = validator.validate(puzzle, emptySet(), 5..5)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
    assertThat((result as ValidationResult.Invalid).reasons.any { "hint" in it && "contains" in it }).isTrue()
  }

  @Test
  fun `hints not containing word are accepted`() {
    val puzzle = createTestPuzzleResponse(word = "gatos")
    val result = validator.validate(puzzle, emptySet(), 5..5)
    assertThat(result).isEqualTo(ValidationResult.Valid)
  }

  // --- Difficulty validation ---

  @Test
  fun `difficulty below 1 is clamped`() {
    val puzzle = createTestPuzzleResponse(difficulty = 0)
    val result = validator.validate(puzzle, emptySet(), 5..5)
    // Clamping means it's still valid, just adjusted
    assertThat(result).isEqualTo(ValidationResult.Valid)
  }

  @Test
  fun `difficulty above 5 is clamped`() {
    val puzzle = createTestPuzzleResponse(difficulty = 10)
    val result = validator.validate(puzzle, emptySet(), 5..5)
    assertThat(result).isEqualTo(ValidationResult.Valid)
  }

  @Test
  fun `difficulty within range is accepted`() {
    val puzzle = createTestPuzzleResponse(difficulty = 3)
    val result = validator.validate(puzzle, emptySet(), 5..5)
    assertThat(result).isEqualTo(ValidationResult.Valid)
  }

  // --- Category validation ---

  @Test
  fun `empty category is rejected`() {
    val puzzle = createTestPuzzleResponse(category = "")
    val result = validator.validate(puzzle, emptySet(), 5..5)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
    assertThat((result as ValidationResult.Invalid).reasons.any { "category" in it }).isTrue()
  }

  @Test
  fun `blank category is rejected`() {
    val puzzle = createTestPuzzleResponse(category = "   ")
    val result = validator.validate(puzzle, emptySet(), 5..5)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
  }

  // --- Multiple validation failures ---

  @Test
  fun `multiple failures are all reported`() {
    val puzzle = PuzzleResponse(
      word = "café",
      category = "",
      difficulty = 3,
      hints = listOf("Dica 1", "Dica 2"),
    )
    val result = validator.validate(puzzle, emptySet(), 5..8)
    assertThat(result).isInstanceOf(ValidationResult.Invalid::class.java)
    val reasons = (result as ValidationResult.Invalid).reasons
    assertThat(reasons.size).isGreaterThan(1)
  }
}

private fun createTestPuzzleResponse(
  word: String = "gatos",
  category: String = "animal",
  difficulty: Int = 2,
  hints: List<String> = listOf(
    "Tem quatro patas",
    "Mia quando quer comida",
    "Persegue ratos",
    "Ronrona feliz",
    "Felino domestico",
  ),
) = PuzzleResponse(word = word, category = category, difficulty = difficulty, hints = hints)
