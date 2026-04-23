package com.woliveiras.palabrita.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
  @PrimaryKey val id: Int = 1,
  val totalPlayed: Int = 0,
  val totalWon: Int = 0,
  val avgAttempts: Float = 0f,
  val preferredLanguage: String = "pt",
  // Legacy columns kept for Room compatibility — no longer used by app logic
  val currentDifficulty: Int = 1,
  val maxUnlockedDifficulty: Int = 1,
  val totalXp: Int = 0,
  val playerTier: String = "NOVATO",
  val gamesWonByDifficulty: String = "{}",
  val winRateByDifficulty: String = "{}",
  val consecutiveLossesAtCurrent: Int = 0,
  val wordSizePreference: String = "DEFAULT",
  val guessDistribution: String = "{}",
  val lastPlayedAt: Long = 0,
)
