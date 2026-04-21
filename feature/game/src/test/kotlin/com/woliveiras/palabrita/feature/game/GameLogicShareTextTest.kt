package com.woliveiras.palabrita.feature.game

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameLogicShareTextTest {

  @Test
  fun `share text contains difficulty stars`() {
    val text = GameLogic.generateShareText(
      attempts = createWinAttempts("gatos", 3),
      difficulty = 3,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 3,
      won = true,
    )
    assertThat(text).contains("⭐⭐⭐")
  }

  @Test
  fun `share text shows attempts fraction on win`() {
    val text = GameLogic.generateShareText(
      attempts = createWinAttempts("gatos", 4),
      difficulty = 1,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 1,
      won = true,
    )
    assertThat(text).contains("4/6")
  }

  @Test
  fun `share text shows X on loss`() {
    val text = GameLogic.generateShareText(
      attempts = createLossAttempts("gatos", 6),
      difficulty = 1,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 0,
      won = false,
    )
    assertThat(text).contains("X/6")
  }

  @Test
  fun `share text contains word`() {
    val text = GameLogic.generateShareText(
      attempts = createWinAttempts("gatos", 2),
      difficulty = 1,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 1,
      won = true,
    )
    assertThat(text).contains("GATOS")
  }

  @Test
  fun `share text contains emoji grid rows matching attempts count`() {
    val attempts = createWinAttempts("gatos", 3)
    val text = GameLogic.generateShareText(
      attempts = attempts,
      difficulty = 1,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 1,
      won = true,
    )
    // Each attempt generates one line of emojis; count lines with colored squares
    val emojiLines = text.lines().filter { line ->
      line.contains("🟦") || line.contains("🟧") || line.contains("🟥")
    }
    assertThat(emojiLines).hasSize(3)
  }

  @Test
  fun `share text maps CORRECT to blue square`() {
    val attempts = listOf(
      Attempt("gatos", List(5) { LetterFeedback('g', LetterState.CORRECT) }),
    )
    val text = GameLogic.generateShareText(
      attempts = attempts,
      difficulty = 1,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 1,
      won = true,
    )
    assertThat(text).contains("🟦🟦🟦🟦🟦")
  }

  @Test
  fun `share text maps PRESENT to orange square`() {
    val attempts = listOf(
      Attempt("togsa", List(5) { LetterFeedback('t', LetterState.PRESENT) }),
    )
    val text = GameLogic.generateShareText(
      attempts = attempts,
      difficulty = 1,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 1,
      won = true,
    )
    assertThat(text).contains("🟧🟧🟧🟧🟧")
  }

  @Test
  fun `share text maps ABSENT to red square`() {
    val attempts = listOf(
      Attempt("xxxxx", List(5) { LetterFeedback('x', LetterState.ABSENT) }),
    )
    val text = GameLogic.generateShareText(
      attempts = attempts,
      difficulty = 1,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 0,
      won = false,
    )
    assertThat(text).contains("🟥🟥🟥🟥🟥")
  }

  @Test
  fun `share text shows hints used`() {
    val text = GameLogic.generateShareText(
      attempts = createWinAttempts("gatos", 2),
      difficulty = 1,
      word = "GATOS",
      hintsUsed = 2,
      xpGained = 1,
      won = true,
    )
    assertThat(text).contains("2 dicas usadas")
  }

  @Test
  fun `share text shows XP on win`() {
    val text = GameLogic.generateShareText(
      attempts = createWinAttempts("gatos", 2),
      difficulty = 3,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 4,
      won = true,
    )
    assertThat(text).contains("+4 XP")
  }

  @Test
  fun `share text does not show XP on loss`() {
    val text = GameLogic.generateShareText(
      attempts = createLossAttempts("gatos", 6),
      difficulty = 1,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 0,
      won = false,
    )
    assertThat(text).doesNotContain("+0 XP")
  }

  @Test
  fun `share text starts with Palabrita`() {
    val text = GameLogic.generateShareText(
      attempts = createWinAttempts("gatos", 1),
      difficulty = 1,
      word = "GATOS",
      hintsUsed = 0,
      xpGained = 4,
      won = true,
    )
    assertThat(text).startsWith("Palabrita")
  }

  // --- Helpers ---

  private fun createWinAttempts(target: String, count: Int): List<Attempt> {
    val wrongAttempts = (1 until count).map {
      Attempt(
        word = "x".repeat(target.length),
        feedback = target.map { LetterFeedback(it, LetterState.ABSENT) },
      )
    }
    val winAttempt = Attempt(
      word = target,
      feedback = target.map { LetterFeedback(it, LetterState.CORRECT) },
    )
    return wrongAttempts + winAttempt
  }

  private fun createLossAttempts(target: String, count: Int): List<Attempt> {
    return (1..count).map {
      Attempt(
        word = "x".repeat(target.length),
        feedback = target.map { LetterFeedback(it, LetterState.ABSENT) },
      )
    }
  }
}
