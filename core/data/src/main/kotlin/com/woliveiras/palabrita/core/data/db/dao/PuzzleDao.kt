package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.woliveiras.palabrita.core.data.db.entity.PuzzleEntity

@Dao
interface PuzzleDao {

  @Query(
    """
        SELECT * FROM puzzles
        WHERE isPlayed = 0 AND language = :lang AND difficulty = :difficulty
        ORDER BY RANDOM()
        LIMIT 1
        """
  )
  suspend fun getNextUnplayed(lang: String, difficulty: Int): PuzzleEntity?

  @Query(
    """
        SELECT COUNT(*) FROM puzzles
        WHERE isPlayed = 0 AND language = :lang AND difficulty = :difficulty
        """
  )
  suspend fun countUnplayed(lang: String, difficulty: Int): Int

  @Query(
    """
        SELECT COUNT(*) FROM puzzles
        WHERE isPlayed = 0 AND language = :lang
        """
  )
  suspend fun countAllUnplayed(lang: String): Int

  @Query("SELECT word FROM puzzles") suspend fun getAllWords(): List<String>

  @Query("SELECT word FROM puzzles ORDER BY generatedAt DESC LIMIT :limit")
  suspend fun getRecentWords(limit: Int): List<String>

  @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(puzzle: PuzzleEntity): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(puzzles: List<PuzzleEntity>)

  @Query("SELECT * FROM puzzles WHERE id = :id") suspend fun getById(id: Long): PuzzleEntity?

  @Query("UPDATE puzzles SET isPlayed = 1, playedAt = :playedAt WHERE id = :id")
  suspend fun markAsPlayed(id: Long, playedAt: Long)

  @Query("DELETE FROM puzzles WHERE isPlayed = 0 AND source = 'AI'")
  suspend fun deleteUnplayedAiPuzzles()

  @Query("UPDATE puzzles SET isPlayed = 0, playedAt = NULL")
  suspend fun markAllUnplayed()
}
