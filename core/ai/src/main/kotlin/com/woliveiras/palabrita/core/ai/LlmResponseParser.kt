package com.woliveiras.palabrita.core.ai

import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class LlmResponseParserImpl @Inject constructor() : LlmResponseParser {

  private val json = Json { ignoreUnknownKeys = true }

  private val jsonObjectRegex = Regex("""\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""")

  private val codeFenceRegex = Regex("""```\w*\s*\n?([\s\S]*?)\n?\s*```""")

  override fun parsePuzzle(rawResponse: String): ParseResult<PuzzleResponse> {
    if (rawResponse.isBlank()) {
      return ParseResult.Error("empty response", rawResponse)
    }

    val cleaned = stripCodeFences(rawResponse)

    // Attempt 1: direct decode with canonical keys
    tryDecode(cleaned)?.let {
      return ParseResult.Success(it)
    }

    // Attempt 2: extract JSON block via regex, then try canonical keys
    val extracted = jsonObjectRegex.find(cleaned)?.value
    if (extracted != null) {
      tryDecode(extracted)?.let {
        return ParseResult.Success(it)
      }
    }

    // Attempt 3: structural detection — identify fields by value type, not key name
    val jsonText = extracted ?: cleaned
    tryStructuralParse(jsonText)?.let {
      return ParseResult.Success(it)
    }

    return ParseResult.Error("could not parse JSON from response", rawResponse)
  }

  private fun stripCodeFences(text: String): String {
    val match = codeFenceRegex.find(text) ?: return text
    return match.groupValues[1].trim()
  }

  private fun tryDecode(text: String): PuzzleResponse? =
    try {
      val response = json.decodeFromString<PuzzleResponse>(text.trim())
      if (response.word.isBlank() || response.hints.isEmpty()) null else response
    } catch (_: Exception) {
      null
    }

  private fun tryStructuralParse(text: String): PuzzleResponse? =
    try {
      val obj = json.decodeFromString<JsonObject>(text.trim())

      var word: String? = null
      var category: String? = null
      var difficulty: Int? = null
      var hints: List<String>? = null

      for ((_, value) in obj) {
        when (value) {
          is JsonArray -> {
            if (hints == null) {
              hints = value.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
            }
          }
          is JsonPrimitive -> {
            val intVal = value.intOrNull
            val strVal = value.contentOrNull
            when {
              intVal != null && difficulty == null -> difficulty = intVal
              strVal != null && word == null && strVal.length in 2..9 && !strVal.contains(' ') ->
                word = strVal
              strVal != null && category == null -> category = strVal
            }
          }
          else -> {}
        }
      }

      if (word != null && category != null && hints != null && hints.isNotEmpty()) {
        PuzzleResponse(
          word = word,
          category = category,
          difficulty = difficulty ?: 1,
          hints = hints,
        )
      } else {
        null
      }
    } catch (_: Exception) {
      null
    }
}

interface LlmResponseParser {
  fun parsePuzzle(rawResponse: String): ParseResult<PuzzleResponse>
}
