package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.LlmSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeLlmEngineManager(
  initialState: EngineState = EngineState.Ready,
  private val sessionResponse: String = "Fake LLM response",
) : LlmEngineManager {
  private val _engineState = MutableStateFlow(initialState)
  override val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

  override suspend fun initialize(modelPath: String) {
    _engineState.value = EngineState.Ready
  }

  override suspend fun generateSingleTurn(systemPrompt: String?, userPrompt: String): String =
    sessionResponse

  override suspend fun createChatSession(systemPrompt: String): LlmSession =
    FakeLlmSession(sessionResponse)

  override fun destroy() {
    _engineState.value = EngineState.Uninitialized
  }

  fun setError(message: String) {
    _engineState.value = EngineState.Error(message)
  }

  override fun isReady(): Boolean = _engineState.value is EngineState.Ready
}

class FakeLlmSession(private val response: String = "Fake LLM response") : LlmSession {
  override suspend fun sendMessage(message: String): String = response

  override fun sendMessageStreaming(message: String): Flow<String> = flowOf(response)

  override fun close() {}
}
