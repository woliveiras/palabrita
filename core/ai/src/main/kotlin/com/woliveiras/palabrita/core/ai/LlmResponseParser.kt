package com.woliveiras.palabrita.core.ai

import javax.inject.Inject
import kotlinx.serialization.json.Json

class LlmResponseParserImpl @Inject constructor() : LlmResponseParser {

  private val json = Json { ignoreUnknownKeys = true }

  private val jsonObjectRegex = Regex("""\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""")

  override fun parsePuzzle(rawResponse: String): ParseResult<PuzzleResponse> {
    if (rawResponse.isBlank()) {
      return ParseResult.Error("empty response", rawResponse)
    }

    // Attempt 1: direct parse
    tryDecode(rawResponse)?.let {
      return ParseResult.Success(it)
    }

    // Attempt 2: extract JSON via regex
    val extracted = jsonObjectRegex.find(rawResponse)?.value
    if (extracted != null) {
      tryDecode(extracted)?.let {
        return ParseResult.Success(it)
      }
    }

    return ParseResult.Error("could not parse JSON from response", rawResponse)
  }

  private fun tryDecode(text: String): PuzzleResponse? =
    try {
      val response = json.decodeFromString<PuzzleResponse>(text.trim())
      if (response.word.isBlank() || response.hints.isEmpty()) null else response
    } catch (_: Exception) {
      null
    }
}

interface LlmResponseParser {
  fun parsePuzzle(rawResponse: String): ParseResult<PuzzleResponse>
}
