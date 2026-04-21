package com.woliveiras.palabrita.core.model.repository

import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.Puzzle

interface ChatRepository {
  suspend fun getMessages(puzzleId: Long): List<ChatMessage>
  suspend fun saveMessage(message: ChatMessage)
  suspend fun countUserMessages(puzzleId: Long): Int
  suspend fun getPuzzle(puzzleId: Long): Puzzle?
  suspend fun deleteAll()
}
