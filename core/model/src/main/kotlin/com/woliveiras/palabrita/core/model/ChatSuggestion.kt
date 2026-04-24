package com.woliveiras.palabrita.core.model

data class ChatSuggestion(
  val icon: String,
  val labelResId: Int,
  val promptTemplate: String,
  val category: SuggestionCategory,
)

enum class SuggestionCategory {
  ETYMOLOGY,
  CURIOSITY,
  USAGE,
  RELATED,
  CULTURAL,
}
