package com.woliveiras.palabrita.core.model

data class PlayerStats(
  val id: Int = 1,
  val totalPlayed: Int = 0,
  val totalWon: Int = 0,
  val avgAttempts: Float = 0f,
  val preferredLanguage: String = "pt",
  val guessDistribution: Map<Int, Int> = emptyMap(),
  val lastPlayedAt: Long = 0,
)
