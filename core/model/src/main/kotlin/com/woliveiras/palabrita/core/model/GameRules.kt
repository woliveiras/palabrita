package com.woliveiras.palabrita.core.model

data class Level(val wordLength: Int, val batchSize: Int, val winsRequired: Int)

object GameRules {
  const val MAX_ATTEMPTS = 6
  const val MIN_HINTS = 3
  const val MAX_WORD_LENGTH = 6
  const val REPLENISHMENT_THRESHOLD = 5
  const val MAX_GENERATION_RETRIES = 5

  /** Level config per generation cycle. Last entry repeats forever. */
  val GENERATION_LEVELS =
    listOf(
      Level(wordLength = 4, batchSize = 5, winsRequired = 5),
      Level(wordLength = 5, batchSize = 10, winsRequired = 10),
      Level(wordLength = 6, batchSize = 10, winsRequired = 10),
    )

  fun levelForCycle(cycle: Int): Level =
    GENERATION_LEVELS[cycle.coerceIn(0, GENERATION_LEVELS.lastIndex)]
}
