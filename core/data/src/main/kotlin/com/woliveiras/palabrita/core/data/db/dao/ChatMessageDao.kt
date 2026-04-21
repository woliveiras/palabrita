package com.woliveiras.palabrita.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.woliveiras.palabrita.core.data.db.entity.ChatMessageEntity

@Dao
interface ChatMessageDao {

    @Insert suspend fun insert(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE puzzleId = :puzzleId ORDER BY timestamp")
    suspend fun getByPuzzleId(puzzleId: Long): List<ChatMessageEntity>

    @Query(
        "SELECT COUNT(*) FROM chat_messages WHERE puzzleId = :puzzleId AND role = 'user'"
    )
    suspend fun countUserMessages(puzzleId: Long): Int
}
