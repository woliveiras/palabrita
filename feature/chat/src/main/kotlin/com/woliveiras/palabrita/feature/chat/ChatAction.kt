package com.woliveiras.palabrita.feature.chat

sealed class ChatAction {
  data class UpdateInput(val text: String) : ChatAction()

  data object SendMessage : ChatAction()

  data class SelectSuggestion(val suggestion: String) : ChatAction()

  data object GoBack : ChatAction()
}
