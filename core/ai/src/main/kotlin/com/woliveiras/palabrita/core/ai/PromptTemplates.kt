package com.woliveiras.palabrita.core.ai

interface PromptProvider {
  fun hintSystemPrompt(language: String): String

  fun hintUserPrompt(word: String, language: String): String

  fun languageDisplayName(code: String): String
}

object PromptTemplates : PromptProvider {

  private var registry: DatasetRegistry? = null

  internal fun init(datasetRegistry: DatasetRegistry) {
    registry = datasetRegistry
  }

  override fun languageDisplayName(code: String): String =
    registry?.promptName(code) ?: code

  override fun hintSystemPrompt(language: String): String {
    val lang = languageDisplayName(language)
    return """
    You write short, clear word-game hints in $lang.

    Rules:
    - The target word is already chosen by the app.
    - Do not change the target word.
    - Write exactly 3 hints.
    - ALL hints MUST be written in $lang. Never write hints in English or any other language.
    - Each hint must describe the meaning or common use of the target word.
    - Do not include the target word in any hint.
    - Do not include spelling, letter count, rhymes, or the first letter.
    - Use simple language for casual mobile players.
    - Return only this format:

    hints: hint 1 | hint 2 | hint 3
    """
      .trimIndent()
  }

  override fun hintUserPrompt(word: String, language: String): String {
    val lang = languageDisplayName(language)
    val upper = word.uppercase()
    return """
      Language: $lang
      Target word: "$upper"

      Write 3 hints for this word IN $lang.
      Do NOT write hints in English unless the language is English.

      Bad hints:
      - "A palavra é $upper"
      - "Começa com ${upper.first()}"
      - "Tem ${word.length} letras"

      Good style:
      hints: <vague hint in $lang> | <medium hint in $lang> | <specific hint in $lang>
    """
      .trimIndent()
  }
}
