package com.woliveiras.palabrita.feature.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.model.ChatMessage
import com.woliveiras.palabrita.core.model.MessageRole
import com.woliveiras.palabrita.core.model.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val chatRepository: ChatRepository,
) : ViewModel() {

  private val puzzleId: Long = savedStateHandle["puzzleId"] ?: 0L

  private val _state = MutableStateFlow(ChatState(puzzleId = puzzleId))
  val state: StateFlow<ChatState> = _state.asStateFlow()

  init {
    loadChat()
  }

  fun onAction(action: ChatAction) {
    when (action) {
      is ChatAction.UpdateInput -> updateInput(action.text)
      is ChatAction.SendMessage -> sendMessage()
      is ChatAction.SelectSuggestion -> selectSuggestion(action.suggestion)
      is ChatAction.GoBack -> { /* handled by UI */ }
    }
  }

  private fun loadChat() {
    viewModelScope.launch {
      val puzzle = chatRepository.getPuzzle(puzzleId) ?: return@launch
      val existing = chatRepository.getMessages(puzzleId)
      val userCount = chatRepository.countUserMessages(puzzleId)

      _state.update {
        it.copy(
          word = puzzle.word,
          category = puzzle.category,
          language = puzzle.language,
          messages = existing.map { msg ->
            UiChatMessage(role = msg.role, content = msg.content)
          },
          userMessageCount = userCount,
          isAtLimit = userCount >= it.maxMessages,
          suggestionsVisible = existing.isEmpty(),
        )
      }

      if (existing.isEmpty()) {
        sendInitialModelMessage()
      }
    }
  }

  private fun sendInitialModelMessage() {
    _state.update { it.copy(isModelResponding = true) }

    viewModelScope.launch {
      val current = _state.value
      // V1: simulate model response (real LLM integration pending)
      val response = "\"${current.word.replaceFirstChar { it.uppercase() }}\" " +
        "é uma palavra da categoria ${current.category}. " +
        "Pergunte qualquer coisa sobre ela!"

      val message = ChatMessage(
        puzzleId = puzzleId,
        role = MessageRole.MODEL,
        content = response,
        timestamp = System.currentTimeMillis(),
      )
      chatRepository.saveMessage(message)

      _state.update {
        it.copy(
          messages = it.messages + UiChatMessage(role = MessageRole.MODEL, content = response),
          isModelResponding = false,
        )
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
        ),
      )

      _state.update {
        it.copy(
          messages = it.messages + UiChatMessage(role = MessageRole.USER, content = userText),
          currentInput = "",
          userMessageCount = newUserCount,
          isAtLimit = newUserCount >= it.maxMessages,
          suggestionsVisible = false,
          isModelResponding = true,
        )
      }

      // V1: simulate model response
      val response = "Boa pergunta sobre \"${_state.value.word}\"! " +
        "Essa é uma resposta simulada enquanto a integração com o LLM está pendente."
      chatRepository.saveMessage(
        ChatMessage(
          puzzleId = puzzleId,
          role = MessageRole.MODEL,
          content = response,
          timestamp = System.currentTimeMillis(),
        ),
      )

      _state.update {
        it.copy(
          messages = it.messages + UiChatMessage(role = MessageRole.MODEL, content = response),
          isModelResponding = false,
        )
      }
    }
  }

  private fun selectSuggestion(suggestion: String) {
    _state.update { it.copy(currentInput = suggestion, suggestionsVisible = false) }
    sendMessage()
  }
}
