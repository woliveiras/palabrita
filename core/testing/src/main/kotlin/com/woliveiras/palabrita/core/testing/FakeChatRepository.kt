package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.MessageRole
import com.woliveiras.palabrita.core.model.repository.ChatRepository

class FakeChatRepository(existingMessages: List<ChatMessage> = emptyList()) : ChatRepository {
  val savedMessages = mutableListOf<ChatMessage>().apply { addAll(existingMessages) }

  override suspend fun getMessages(puzzleId: Long): List<ChatMessage> = savedMessages.filter {
    it.puzzleId == puzzleId
  }

  override suspend fun saveMessage(message: ChatMessage) {
    savedMessages.add(message)
  }

  override suspend fun countUserMessages(puzzleId: Long): Int = savedMessages.count {
    it.puzzleId == puzzleId && it.role == MessageRole.USER
  }

  override suspend fun deleteAll() {
    savedMessages.clear()
  }
}
