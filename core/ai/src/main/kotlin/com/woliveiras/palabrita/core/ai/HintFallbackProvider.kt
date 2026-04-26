package com.woliveiras.palabrita.core.ai

import javax.inject.Inject

/**
 * Generates generic template-based hints when the LLM fails. These hints are intentionally vague
 * but always valid — they never contain the target word and always produce exactly 3 hints.
 *
 * This guarantees 100% puzzle generation success: the word comes from a curated list and hints
 * always have a fallback.
 */
interface HintFallbackProvider {
  fun fallbackHints(word: String, language: String): List<String>
}

class HintFallbackProviderImpl @Inject constructor() : HintFallbackProvider {

  override fun fallbackHints(word: String, language: String): List<String> =
    when (language) {
      "pt" ->
        listOf(
          "É algo que as pessoas conhecem",
          "Pode ser encontrado no dia a dia",
          "Tem ${word.length} letras",
        )
      "es" ->
        listOf(
          "Es algo que la gente conoce",
          "Se puede encontrar en el día a día",
          "Tiene ${word.length} letras",
        )
      else ->
        listOf(
          "It is something people know",
          "It can be found in everyday life",
          "It has ${word.length} letters",
        )
    }
}
