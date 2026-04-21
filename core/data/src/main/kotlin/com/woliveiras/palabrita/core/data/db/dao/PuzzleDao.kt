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
        ORDER BY generatedAt LIMIT 1
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

  @Query("SELECT word FROM puzzles") suspend fun getAllWords(): List<String>

  @Query("SELECT word FROM puzzles ORDER BY generatedAt DESC LIMIT :limit")
  suspend fun getRecentWords(limit: Int): List<String>

  @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(puzzle: PuzzleEntity): Long

  @Query("UPDATE puzzles SET isPlayed = 1, playedAt = :playedAt WHERE id = :id")
  suspend fun markAsPlayed(id: Long, playedAt: Long)
}
