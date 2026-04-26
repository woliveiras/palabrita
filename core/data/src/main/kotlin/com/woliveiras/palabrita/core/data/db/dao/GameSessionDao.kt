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

  @Query("SELECT * FROM game_sessions WHERE completedAt IS NOT NULL ORDER BY completedAt DESC")
  suspend fun getCompletedSessionsDesc(): List<GameSessionEntity>

  @Query(
    """
    SELECT COUNT(*) FROM game_sessions gs
    INNER JOIN puzzles p ON gs.puzzleId = p.id
    WHERE gs.won = 1
      AND gs.completedAt IS NOT NULL
      AND p.difficulty = :difficulty
      AND p.language = :language
    """
  )
  suspend fun countWinsByDifficulty(difficulty: Int, language: String): Int

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
