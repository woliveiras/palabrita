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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class LlmEngineManagerImpl @Inject constructor(@ApplicationContext private val context: Context) :
  LlmEngineManager {

  private var engine: Engine? = null
  private var modelPath: String? = null
  private var usingGpu: Boolean = false
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
        val newEngine = initializeWithFallback(modelPath)
        mutex.withLock {
          engine?.close()
          engine = newEngine
          this@LlmEngineManagerImpl.modelPath = modelPath
          _engineState.value = EngineState.Ready
        }
      }
    } catch (e: Exception) {
      mutex.withLock {
        android.util.Log.e("LlmEngineManager", "Failed to initialize engine", e)
        _engineState.value =
          EngineState.Error(
            context.getString(com.woliveiras.palabrita.core.common.R.string.error_engine_init)
          )
      }
    }
  }

  /**
   * Attempts GPU backend first; falls back to CPU if GPU is unavailable. On emulators, GPU is
   * skipped entirely because LiteRT-LM's GPU init succeeds via WebGPU/llvmpipe but inference fails
   * (missing OpenCL), and the resulting native state makes it impossible to safely close the engine
   * or create a second one (SIGABRT).
   */
  private fun initializeWithFallback(modelPath: String): Engine {
    val backends =
      if (isEmulator()) {
        android.util.Log.i("LlmEngineManager", "Emulator detected, using CPU backend only")
        listOf(Backend.CPU())
      } else {
        listOf(Backend.GPU(), Backend.CPU())
      }
    var lastException: Exception? = null
    for (backend in backends) {
      try {
        val config =
          EngineConfig(
            modelPath = modelPath,
            backend = backend,
            cacheDir = context.cacheDir.absolutePath,
          )
        val engine = Engine(config)
        engine.initialize()
        usingGpu = backend is Backend.GPU
        android.util.Log.i(
          "LlmEngineManager",
          "Initialized with backend: ${backend::class.simpleName}",
        )
        return engine
      } catch (e: Exception) {
        android.util.Log.w(
          "LlmEngineManager",
          "Backend ${backend::class.simpleName} unavailable, trying next",
          e,
        )
        lastException = e
      }
    }
    throw lastException ?: IllegalStateException("No available backend")
  }

  override suspend fun generateSingleTurn(systemPrompt: String?, userPrompt: String): String {
    val currentEngine = engine ?: throw IllegalStateException("Engine not initialized")

    return withContext(Dispatchers.IO) { doGenerate(currentEngine, systemPrompt, userPrompt) }
  }

  private fun doGenerate(eng: Engine, systemPrompt: String?, userPrompt: String): String {
    val conversationConfig =
      ConversationConfig(
        systemInstruction = systemPrompt?.let { Contents.of(it) },
        samplerConfig = SamplerConfig(topK = 40, topP = 0.95, temperature = 0.8),
      )
    return eng.createConversation(conversationConfig).use { conversation ->
      val message = conversation.sendMessage(userPrompt)
      message.toString()
    }
  }

  override fun destroy() {
    engine?.close()
    engine = null
    modelPath = null
    usingGpu = false
    _engineState.value = EngineState.Uninitialized
  }

  override fun isReady(): Boolean = _engineState.value is EngineState.Ready

  @Suppress("KotlinConstantConditions")
  private fun isEmulator(): Boolean {
    val fingerprint = android.os.Build.FINGERPRINT
    val model = android.os.Build.MODEL
    val hardware = android.os.Build.HARDWARE
    val product = android.os.Build.PRODUCT
    android.util.Log.d(
      "LlmEngineManager",
      "isEmulator check: FINGERPRINT=$fingerprint, MODEL=$model, HARDWARE=$hardware, PRODUCT=$product",
    )
    val result =
      fingerprint.contains("generic") ||
        fingerprint.contains("emulator") ||
        model.contains("Emulator") ||
        model.contains("Android SDK built for") ||
        hardware.contains("goldfish") ||
        hardware.contains("ranchu") ||
        product.contains("sdk_gphone") ||
        android.os.Build.BRAND == "generic"
    android.util.Log.d("LlmEngineManager", "isEmulator result: $result")
    return result
  }
}
