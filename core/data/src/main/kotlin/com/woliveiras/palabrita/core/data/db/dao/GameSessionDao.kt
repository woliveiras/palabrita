package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.woliveiras.palabrita.core.data.db.entity.GameSessionEntity

@Dao
interface GameSessionDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: GameSessionEntity): Long

  @Update suspend fun update(session: GameSessionEntity)

  @Query("SELECT * FROM game_sessions WHERE puzzleId = :puzzleId")
  suspend fun getByPuzzleId(puzzleId: Long): GameSessionEntity?

  @Query("SELECT * FROM game_sessions WHERE completedAt IS NULL LIMIT 1")
  suspend fun getActiveSession(): GameSessionEntity?

  @Query("SELECT EXISTS(SELECT 1 FROM game_sessions WHERE completedAt IS NULL)")
  suspend fun hasActiveGame(): Boolean

  @Query("DELETE FROM game_sessions") suspend fun deleteAll()

  @Query(
    """
    UPDATE game_sessions SET
      attempts = :attempts,
      completedAt = :completedAt,
      hintsUsed = :hintsUsed,
      won = :won
    WHERE puzzleId = :puzzleId
    """
  )
  suspend fun completeSession(
    puzzleId: Long,
    attempts: String,
    completedAt: Long,
    hintsUsed: Int,
    won: Boolean,
  )
}
