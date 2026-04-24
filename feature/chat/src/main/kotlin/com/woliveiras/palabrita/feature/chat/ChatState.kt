package com.woliveiras.palabrita.feature.chat

import com.woliveiras.palabrita.core.model.GameRules
import com.woliveiras.palabrita.core.model.MessageRole

data class ChatState(
  val puzzleId: Long = 0,
  val word: String = "",
  val language: String = "",
  val messages: List<UiChatMessage> = emptyList(),
  val currentInput: String = "",
  val isModelResponding: Boolean = false,
  val isEngineLoading: Boolean = false,
  val userMessageCount: Int = 0,
  val maxMessages: Int = GameRules.MAX_CHAT_MESSAGES,
  val isAtLimit: Boolean = false,
  val error: String? = null,
  val suggestionsVisible: Boolean = true,
)

data class UiChatMessage(
  val role: MessageRole,
  val content: String,
  val isStreaming: Boolean = false,
)
