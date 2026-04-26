package com.woliveiras.palabrita.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions", indices = [Index(value = ["puzzleId"], unique = true)])
data class GameSessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val puzzleId: Long,
  val attempts: String = "[]",
  val startedAt: Long,
  val completedAt: Long? = null,
  val hintsUsed: Int = 0,
  val won: Boolean = false,
)
