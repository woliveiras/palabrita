package com.woliveiras.palabrita.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.LlmSession
import com.woliveiras.palabrita.core.ai.PromptTemplates
import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.MessageRole
import com.woliveiras.palabrita.core.model.repository.ChatRepository
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val RESPONSE_TIMEOUT_MS = 60_000L
private const val ENGINE_INIT_TIMEOUT_MS = 120_000L
private const val INITIAL_PROMPT = "Conte uma curiosidade interessante sobre a palavra '{word}'"

@HiltViewModel
class ChatViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val chatRepository: ChatRepository,
  private val puzzleRepository: PuzzleRepository,
  private val engineManager: LlmEngineManager,
  private val modelRepository: ModelRepository,
) : ViewModel() {

  private val puzzleId: Long = savedStateHandle["puzzleId"] ?: 0L

  private val _state = MutableStateFlow(ChatState(puzzleId = puzzleId))
  val state: StateFlow<ChatState> = _state.asStateFlow()

  private var session: LlmSession? = null
  private var streamingJob: Job? = null

  init {
    loadChat()
  }

  fun onAction(action: ChatAction) {
    when (action) {
      is ChatAction.UpdateInput -> updateInput(action.text)
      is ChatAction.SendMessage -> sendMessage()
      is ChatAction.SelectSuggestion -> selectSuggestion(action.suggestion)
      is ChatAction.GoBack -> {
        /* handled by UI */
      }
    }
  }

  private fun loadChat() {
    viewModelScope.launch {
      val puzzle = puzzleRepository.getById(puzzleId) ?: return@launch
      val existing = chatRepository.getMessages(puzzleId)
      val userCount = chatRepository.countUserMessages(puzzleId)

      _state.update {
        it.copy(
          word = puzzle.word,
          language = puzzle.language,
          messages = existing.map { msg -> UiChatMessage(role = msg.role, content = msg.content) },
          userMessageCount = userCount,
          isAtLimit = userCount >= it.maxMessages,
          suggestionsVisible = existing.isEmpty(),
        )
      }

      if (existing.isEmpty()) {
        ensureSessionAndSend(INITIAL_PROMPT.replace("{word}", puzzle.word), isInitial = true)
      } else {
        replayHistoryIntoSession(existing)
      }
    }
  }

  private suspend fun replayHistoryIntoSession(messages: List<ChatMessage>) {
    if (!ensureSession()) return
    val currentSession = session ?: return
    for (msg in messages) {
      try {
        if (msg.role == MessageRole.USER) {
          currentSession.sendMessage(msg.content)
        }
      } catch (_: Exception) {
        break
      }
    }
  }

  private suspend fun ensureSession(): Boolean {
    if (session != null) return true

    val currentEngineState = engineManager.engineState.value
    if (currentEngineState !is EngineState.Ready) {
      _state.update { it.copy(isEngineLoading = true, error = null) }

      if (currentEngineState is EngineState.Uninitialized) {
        val config = modelRepository.getConfig()
        val modelPath = config.modelPath
        if (modelPath == null) {
          _state.update { it.copy(isEngineLoading = false, error = "No AI model configured.") }
          return false
        }
        engineManager.initialize(modelPath)
      }

      val ready =
        withTimeoutOrNull(ENGINE_INIT_TIMEOUT_MS) {
          engineManager.engineState.first { it is EngineState.Ready || it is EngineState.Error }
        }

      _state.update { it.copy(isEngineLoading = false) }

      if (ready == null || ready is EngineState.Error) {
        val errorMsg =
          if (ready is EngineState.Error) ready.message else "AI engine initialization timed out."
        _state.update { it.copy(error = errorMsg) }
        return false
      }
    }

    val current = _state.value
    val systemPrompt = PromptTemplates.chatSystemPrompt(current.word, current.language)
    session = engineManager.createChatSession(systemPrompt)
    return true
  }

  private fun ensureSessionAndSend(userText: String, isInitial: Boolean = false) {
    _state.update { it.copy(isModelResponding = true, error = null) }

    streamingJob = viewModelScope.launch {
      if (!ensureSession()) {
        _state.update { it.copy(isModelResponding = false) }
        return@launch
      }

      val currentSession = session ?: return@launch

      try {
        val accumulated = StringBuilder()

        _state.update {
          it.copy(
            messages =
              it.messages +
                UiChatMessage(role = MessageRole.MODEL, content = "", isStreaming = true)
          )
        }

        val completed =
          withTimeoutOrNull(RESPONSE_TIMEOUT_MS) {
            currentSession.sendMessageStreaming(userText).collect { token ->
              accumulated.append(token)
              val text = accumulated.toString()
              _state.update {
                val updated = it.messages.toMutableList()
                updated[updated.lastIndex] =
                  UiChatMessage(role = MessageRole.MODEL, content = text, isStreaming = true)
                it.copy(messages = updated)
              }
            }
          }

        val finalText = accumulated.toString()

        if (completed == null && finalText.isBlank()) {
          _state.update {
            val updated = it.messages.toMutableList()
            updated.removeAt(updated.lastIndex)
            it.copy(
              messages = updated,
              isModelResponding = false,
              error = "Response timed out. Try again.",
            )
          }
          return@launch
        }

        _state.update {
          val updated = it.messages.toMutableList()
          updated[updated.lastIndex] =
            UiChatMessage(role = MessageRole.MODEL, content = finalText, isStreaming = false)
          it.copy(messages = updated, isModelResponding = false)
        }

        chatRepository.saveMessage(
          ChatMessage(
            puzzleId = puzzleId,
            role = MessageRole.MODEL,
            content = finalText,
            timestamp = System.currentTimeMillis(),
          )
        )
      } catch (e: Exception) {
        _state.update {
          val updated = it.messages.toMutableList()
          if (updated.isNotEmpty() && updated.last().isStreaming) {
            updated.removeAt(updated.lastIndex)
          }
          it.copy(
            messages = updated,
            isModelResponding = false,
            error = "Failed to get response: ${e.message}",
          )
        }
      }
    }
  }

  private fun updateInput(text: String) {
    _state.update { it.copy(currentInput = text) }
  }

  private fun sendMessage() {
    val current = _state.value
    if (current.currentInput.isBlank()) return
    if (current.isAtLimit) return
    if (current.isModelResponding) return

    val userText = current.currentInput.trim()
    val newUserCount = current.userMessageCount + 1

    viewModelScope.launch {
      chatRepository.saveMessage(
        ChatMessage(
          puzzleId = puzzleId,
          role = MessageRole.USER,
          content = userText,
          timestamp = System.currentTimeMillis(),
        )
      )

      _state.update {
        it.copy(
          messages = it.messages + UiChatMessage(role = MessageRole.USER, content = userText),
          currentInput = "",
          userMessageCount = newUserCount,
          isAtLimit = newUserCount >= it.maxMessages,
          suggestionsVisible = false,
        )
      }

      ensureSessionAndSend(userText)
    }
  }

  private fun selectSuggestion(suggestion: String) {
    _state.update { it.copy(currentInput = suggestion, suggestionsVisible = false) }
    sendMessage()
  }

  override fun onCleared() {
    super.onCleared()
    streamingJob?.cancel()
    try {
      session?.close()
    } catch (_: Exception) {}
    session = null
  }
}
