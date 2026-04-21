package com.woliveiras.palabrita.core.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class LlmEngineManagerImpl @Inject constructor(
  @ApplicationContext private val context: Context,
) : LlmEngineManager {

  private var engine: Engine? = null
  private val mutex = Mutex()

  private val _engineState = MutableStateFlow<EngineState>(EngineState.Uninitialized)
  override val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

  override suspend fun initialize(modelPath: String) {
    mutex.withLock {
      if (_engineState.value is EngineState.Ready) return
      _engineState.value = EngineState.Initializing
    }

    try {
      withContext(Dispatchers.IO) {
        val config =
          EngineConfig(
            modelPath = modelPath,
            backend = Backend.CPU(),
            cacheDir = context.cacheDir.absolutePath,
          )
        val newEngine = Engine(config)
        newEngine.initialize()

        mutex.withLock {
          engine?.close()
          engine = newEngine
          _engineState.value = EngineState.Ready
        }
      }
    } catch (e: Exception) {
      mutex.withLock { _engineState.value = EngineState.Error(e.message ?: "Engine init failed") }
    }
  }

  override suspend fun generateSingleTurn(systemPrompt: String?, userPrompt: String): String {
    val currentEngine =
      engine ?: throw IllegalStateException("Engine not initialized")

    return withContext(Dispatchers.IO) {
      val conversationConfig =
        ConversationConfig(
          systemInstruction =
            systemPrompt?.let { Contents.of(it) },
          samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8),
        )

      currentEngine.createConversation(conversationConfig).use { conversation ->
        val message = conversation.sendMessage(userPrompt)
        message.toString()
      }
    }
  }

  override suspend fun createChatSession(systemPrompt: String): LlmSession {
    val currentEngine =
      engine ?: throw IllegalStateException("Engine not initialized")

    return withContext(Dispatchers.IO) {
      val conversationConfig =
        ConversationConfig(
          systemInstruction = Contents.of(systemPrompt),
          samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8),
        )

      val conversation = currentEngine.createConversation(conversationConfig)
      LlmSessionImpl(conversation)
    }
  }

  override fun destroy() {
    engine?.close()
    engine = null
    _engineState.value = EngineState.Uninitialized
  }

  override fun isReady(): Boolean = _engineState.value is EngineState.Ready
}

private class LlmSessionImpl(
  private val conversation: com.google.ai.edge.litertlm.Conversation,
) : LlmSession {

  override suspend fun sendMessage(message: String): String {
    return withContext(Dispatchers.IO) {
      val response = conversation.sendMessage(message)
      response.toString()
    }
  }

  override fun sendMessageStreaming(message: String): Flow<String> {
    return conversation.sendMessageAsync(message).map { msg ->
      msg.toString()
    }
  }

  override fun close() {
    conversation.close()
  }
}
