package com.woliveiras.palabrita.core.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

sealed class EngineState {
  data object Uninitialized : EngineState()

  data object Initializing : EngineState()

  data object Ready : EngineState()

  data class Error(val message: String) : EngineState()
}

interface LlmSession : AutoCloseable {
  suspend fun sendMessage(message: String): String

  fun sendMessageStreaming(message: String): Flow<String>
}

interface LlmEngineManager {
  val engineState: StateFlow<EngineState>

  suspend fun initialize(modelPath: String)

  suspend fun generateSingleTurn(systemPrompt: String?, userPrompt: String): String

  suspend fun createChatSession(systemPrompt: String): LlmSession

  fun destroy()

  fun isReady(): Boolean
}
