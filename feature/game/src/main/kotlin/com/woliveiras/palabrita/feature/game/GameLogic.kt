package com.woliveiras.palabrita.feature.game

object GameLogic {

  fun calculateLetterFeedback(guess: String, target: String): List<LetterFeedback> {
    val result = Array(guess.length) { LetterFeedback(guess[it], LetterState.ABSENT) }
    val targetCounts = mutableMapOf<Char, Int>()
    target.forEach { targetCounts[it] = (targetCounts[it] ?: 0) + 1 }

    // Pass 1: mark CORRECT
    for (i in guess.indices) {
      if (guess[i] == target[i]) {
        result[i] = LetterFeedback(guess[i], LetterState.CORRECT)
        targetCounts[guess[i]] = targetCounts[guess[i]]!! - 1
      }
    }

    // Pass 2: mark PRESENT (left to right)
    for (i in guess.indices) {
      if (result[i].state == LetterState.CORRECT) continue
      val remaining = targetCounts[guess[i]] ?: 0
      if (remaining > 0) {
        result[i] = LetterFeedback(guess[i], LetterState.PRESENT)
        targetCounts[guess[i]] = remaining - 1
      }
    }

    return result.toList()
  }

  fun updateKeyboardState(
    current: Map<Char, LetterState>,
    feedback: List<LetterFeedback>,
  ): Map<Char, LetterState> {
    val result = current.toMutableMap()
    for (fb in feedback) {
      val existing = result[fb.letter]
      val newState = fb.state
      result[fb.letter] =
        when {
          existing == null -> newState
          existing == LetterState.CORRECT -> LetterState.CORRECT
          newState == LetterState.CORRECT -> LetterState.CORRECT
          existing == LetterState.PRESENT -> LetterState.PRESENT
          newState == LetterState.PRESENT -> LetterState.PRESENT
          else -> newState
        }
    }
    return result
  }

  fun generateShareText(
    attempts: List<Attempt>,
    difficulty: Int,
    word: String,
    hintsUsed: Int,
    won: Boolean,
  ): String {
    val stars = "⭐".repeat(difficulty)
    val score = if (won) "${attempts.size}/6" else "X/6"
    val emojiGrid =
      attempts.joinToString("\n") { attempt ->
        attempt.feedback.joinToString("") { fb ->
          when (fb.state) {
            LetterState.CORRECT -> "🟦"
            LetterState.PRESENT -> "🟧"
            LetterState.ABSENT -> "🟥"
            LetterState.UNUSED -> "⬜"
          }
        }
      }

    val footer = buildString {
      append("A palavra era: $word")
      if (hintsUsed > 0) {
        append("\n$hintsUsed dicas usadas")
      }
    }

    return "Palabrita $stars — $score\n\n$emojiGrid\n\n$footer"
  }
}
