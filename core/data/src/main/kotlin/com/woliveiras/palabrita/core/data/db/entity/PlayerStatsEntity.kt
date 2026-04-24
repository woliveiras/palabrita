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
  val guessDistribution: String = "{}",
  val lastPlayedAt: Long = 0,
)
