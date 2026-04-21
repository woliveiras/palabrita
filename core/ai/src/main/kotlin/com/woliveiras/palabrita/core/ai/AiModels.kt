package com.woliveiras.palabrita.core.ai

import kotlinx.serialization.Serializable

@Serializable
data class PuzzleResponse(
  val word: String,
  val category: String,
  val difficulty: Int,
  val hints: List<String>,
)

sealed class ParseResult<out T> {
  data class Success<T>(val data: T) : ParseResult<T>()

  data class Error(val reason: String, val rawResponse: String) : ParseResult<Nothing>()
}

sealed class ValidationResult {
  data object Valid : ValidationResult()

  data class Invalid(val reasons: List<String>) : ValidationResult()
}
