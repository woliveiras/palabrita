package com.woliveiras.palabrita.core.ai

import com.woliveiras.palabrita.core.model.GameRules
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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

    val sanitized = sanitizeUtf8(rawResponse)
    val cleaned = stripCodeFences(sanitized)

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

  private fun sanitizeUtf8(text: String): String {
    val bytes = text.toByteArray(Charsets.UTF_8)
    return String(bytes, Charsets.UTF_8).replace("\uFFFD", "")
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
      var hints: List<String>? = null

      for ((_, value) in obj) {
        when (value) {
          is JsonArray -> {
            if (hints == null) {
              hints = value.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
            }
          }
          is JsonPrimitive -> {
            val strVal = value.contentOrNull
            if (strVal != null && word == null && strVal.length in 2..8 && !strVal.contains(' ')) {
              word = strVal
            }
          }
          else -> {}
        }
      }

      if (word != null && hints != null && hints.size >= GameRules.MIN_HINTS) {
        PuzzleResponse(word = word, hints = hints)
      } else {
        null
      }
    } catch (_: Exception) {
      null
    }

  /**
   * Parses a hint-only response from the LLM. Expected format:
   * ```
   * hints: hint 1 | hint 2 | hint 3
   * ```
   *
   * Tolerates extra prose lines, markdown fences, and localized keys.
   */
  override fun parseHints(rawResponse: String): ParseResult<List<String>> {
    if (rawResponse.isBlank()) {
      return ParseResult.Error("empty response", rawResponse)
    }

    val sanitized = sanitizeUtf8(rawResponse)
    val cleaned = stripCodeFences(sanitized)

    // Look for a line starting with a hints-like key followed by colon
    val hintsKeys = setOf("hints", "dicas", "pistas", "clues", "indices", "consejos")
    for (line in cleaned.lines()) {
      val trimmed = line.trim()
      val colonIndex = trimmed.indexOf(':')
      if (colonIndex < 0) continue

      val key = trimmed.substring(0, colonIndex).trim().lowercase()
      if (key in hintsKeys) {
        val value = trimmed.substring(colonIndex + 1).trim()
        val hints = value.split("|").map { it.trim() }.filter { it.isNotBlank() }
        if (hints.size >= GameRules.MIN_HINTS) {
          return ParseResult.Success(hints)
        }
      }
    }

    return ParseResult.Error("could not parse hints from response", rawResponse)
  }
}

interface LlmResponseParser {
  fun parsePuzzle(rawResponse: String): ParseResult<PuzzleResponse>

  fun parseHints(rawResponse: String): ParseResult<List<String>>
}
