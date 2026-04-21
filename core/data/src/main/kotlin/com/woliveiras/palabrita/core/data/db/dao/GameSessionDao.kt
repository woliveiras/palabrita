package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.woliveiras.palabrita.core.data.db.entity.GameSessionEntity

@Dao
interface GameSessionDao {

  @Insert suspend fun insert(session: GameSessionEntity): Long

  @Update suspend fun update(session: GameSessionEntity)

  @Query("SELECT * FROM game_sessions WHERE puzzleId = :puzzleId")
  suspend fun getByPuzzleId(puzzleId: Long): GameSessionEntity?
}
