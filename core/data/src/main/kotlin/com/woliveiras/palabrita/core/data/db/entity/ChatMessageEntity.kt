package com.woliveiras.palabrita.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages", indices = [Index(value = ["puzzleId", "timestamp"])])
data class ChatMessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val puzzleId: Long,
  val role: String,
  val content: String,
  val timestamp: Long,
)
