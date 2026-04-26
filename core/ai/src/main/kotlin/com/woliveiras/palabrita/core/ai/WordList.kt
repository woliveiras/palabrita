package com.woliveiras.palabrita.core.ai

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Curated word lists loaded from JSON resource files (`wordlists/{lang}.json`).
 * Words are guaranteed to be:
 * - Real, common nouns that native speakers recognize immediately
 * - Exactly the declared length (after diacritic normalization to ASCII)
 * - Free of verbs, adjectives, proper nouns, or obscure terms
 *
 * The LLM is NOT responsible for choosing words — only for generating hints.
 * This eliminates greedy-decoding loop issues and invented words.
 */
object WordList {

  private const val TAG = "WordList"

  private val cache = mutableMapOf<String, Map<Int, List<String>>>()

  /**
   * Returns the full word list for the given [language] and [wordLength], or an empty list if no
   * words are available for that combination.
   */
  fun getWords(language: String, wordLength: Int): List<String> =
    loadLanguage(language)[wordLength].orEmpty()

  private fun loadLanguage(language: String): Map<Int, List<String>> =
    cache.getOrPut(language) { parseResource(language) }

  private fun parseResource(language: String): Map<Int, List<String>> {
    val stream =
      WordList::class.java.getResourceAsStream("/wordlists/$language.json")
        ?: run {
          Log.w(TAG, "No word list found for language: $language")
          return emptyMap()
        }

    return try {
      val text = stream.bufferedReader().use { it.readText() }
      val jsonObject = Json.parseToJsonElement(text) as JsonObject
      jsonObject.entries.associate { (key, value) ->
        key.toInt() to value.jsonArray.map { it.jsonPrimitive.content }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to parse word list for language: $language", e)
      emptyMap()
    }
  }
}
