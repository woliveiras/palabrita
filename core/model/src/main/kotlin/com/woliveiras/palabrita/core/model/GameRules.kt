package com.woliveiras.palabrita.core.model

object GameRules {
  const val MAX_ATTEMPTS = 6
  const val MIN_HINTS = 3
  const val MAX_CHAT_MESSAGES = 10
  const val MAX_WORD_LENGTH = 6
  const val REPLENISHMENT_THRESHOLD = 5
  const val MAX_GENERATION_RETRIES = 5

  /** (wordLength, batchSize) per generation cycle. Last entry repeats forever. */
  val GENERATION_LEVELS = listOf(4 to 5, 5 to 10, 6 to 10)

  fun levelForCycle(cycle: Int): Pair<Int, Int> =
    GENERATION_LEVELS[cycle.coerceIn(0, GENERATION_LEVELS.lastIndex)]
}
