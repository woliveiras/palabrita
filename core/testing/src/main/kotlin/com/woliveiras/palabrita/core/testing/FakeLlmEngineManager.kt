package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

  override fun destroy() {
    _engineState.value = EngineState.Uninitialized
  }

  fun setError(message: String) {
    _engineState.value = EngineState.Error(message)
  }

  override fun isReady(): Boolean = _engineState.value is EngineState.Ready
}
