package com.woliveiras.palabrita.core.ai

interface PromptProvider {
  fun hintSystemPrompt(): String

  fun hintUserPrompt(word: String, language: String): String

  fun chatSystemPrompt(word: String, language: String): String

  fun languageDisplayName(code: String): String
}

object PromptTemplates : PromptProvider {

  private val LANGUAGE_NAMES =
    mapOf("pt" to "Brazilian Portuguese", "en" to "English", "es" to "Spanish")

  override fun languageDisplayName(code: String): String = LANGUAGE_NAMES[code] ?: code

  override fun hintSystemPrompt(): String =
    """
    You write short, clear word-game hints.

    Rules:
    - The target word is already chosen by the app.
    - Do not change the target word.
    - Write exactly 3 hints.
    - Each hint must describe the meaning or common use of the target word.
    - Do not include the target word in any hint.
    - Do not include spelling, letter count, rhymes, or the first letter.
    - Use simple language for casual mobile players.
    - Return only this format:

    hints: hint 1 | hint 2 | hint 3
    """
      .trimIndent()

  override fun hintUserPrompt(word: String, language: String): String {
    val lang = languageDisplayName(language)
    val upper = word.uppercase()
    return """
      Language: $lang
      Target word: "$upper"

      Write 3 hints for this word.

      Bad hints:
      - "A palavra é $upper"
      - "Começa com ${upper.first()}"
      - "Tem ${word.length} letras"

      Good style:
      hints: <vague hint> | <medium hint> | <specific hint>
    """
      .trimIndent()
  }

  override fun chatSystemPrompt(word: String, language: String): String {
    val lang = languageDisplayName(language)
    return """
    You are an educational assistant. The player just guessed the word "$word".
    Answer questions about: word origin, etymology, fun facts, usage in sentences, synonyms, translations to other languages.
    Keep responses short (max 3 paragraphs). Always respond in $lang.
    """
      .trimIndent()
  }
}
