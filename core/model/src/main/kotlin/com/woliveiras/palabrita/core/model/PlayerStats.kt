package com.woliveiras.palabrita.core.model

data class PlayerStats(
  val id: Int = 1,
  val totalPlayed: Int = 0,
  val totalWon: Int = 0,
  val avgAttempts: Float = 0f,
  val preferredLanguage: String = "pt",
  val currentDifficulty: Int = 1,
  val maxUnlockedDifficulty: Int = 1,
  val totalXp: Int = 0,
  val playerTier: PlayerTier = PlayerTier.NOVATO,
  val gamesWonByDifficulty: Map<Int, Int> = emptyMap(),
  val winRateByDifficulty: Map<Int, Float> = emptyMap(),
  val consecutiveLossesAtCurrent: Int = 0,
  val wordSizePreference: String = "DEFAULT",
  val guessDistribution: Map<Int, Int> = emptyMap(),
  val lastPlayedAt: Long = 0,
)
