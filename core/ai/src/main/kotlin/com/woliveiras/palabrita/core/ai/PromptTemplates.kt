package com.woliveiras.palabrita.core.ai

object PromptTemplates {

  fun puzzleSystemPromptGemma4(): String =
    """
    You are a word generator for a guessing game.
    Always respond with ONLY a JSON object. No markdown, no code fences, no explanation.
    The JSON keys MUST be in English: "word", "category", "difficulty", "hints".
    The values for word, category, and hints must be in the requested language.
    """
      .trimIndent()

  fun puzzleUserPromptGemma4(
    language: String,
    difficulty: Int,
    minLength: Int,
    maxLength: Int,
    recentWords: List<String>,
  ): String {
    val rarity = difficultyToRarity(difficulty)
    return """
      Generate a word for the game. Return ONLY a JSON object, no markdown.

      Required JSON format (keys MUST be in English):
      {"word": "string", "category": "string", "difficulty": number, "hints": ["h1","h2","h3","h4","h5"]}

      CRITICAL: The word MUST have exactly $minLength to $maxLength letters. Count the letters carefully. Words with fewer or more letters will be rejected.

      Rules:
      - Output language for values: $language
      - The word MUST be a common noun in $language, $minLength-$maxLength lowercase letters, no accents
      - Word rarity: $rarity
      - difficulty: $difficulty (1=easy, 5=hard)
      - Exactly 5 progressive hints in $language: from vaguest to most specific
      - Hints MUST NOT contain the word itself
      - Avoid these recent words: ${recentWords.joinToString(", ")}
    """
      .trimIndent()
  }

  fun puzzlePromptGemma3(
    language: String,
    difficulty: Int,
    minLength: Int,
    maxLength: Int,
    recentWords: List<String>,
  ): String {
    val rarity = difficultyToRarity(difficulty)
    return """
      You are a word generator for a game. Return ONLY a JSON object, no markdown, no code fences.

      Required JSON format (keys MUST be in English exactly as shown):
      {"word": "string", "category": "string", "difficulty": number, "hints": ["h1","h2","h3","h4","h5"]}

      Rules:
      - The word MUST be a common noun in $language, $minLength-$maxLength lowercase letters, no accents
      - Word rarity: $rarity
      - No proper nouns
      - difficulty: $difficulty (1=easy, 5=hard)
      - Exactly 5 progressive hints in $language: from vaguest to most specific
      - Hints MUST NOT contain the word itself
      - Avoid these recent words: ${recentWords.joinToString(", ")}
    """
      .trimIndent()
  }

  fun chatSystemPrompt(word: String, category: String, language: String): String =
    """
    You are an educational assistant. The player just guessed the word "$word" (category: $category).
    Answer questions about: word origin, etymology, fun facts, usage in sentences, synonyms, translations to other languages.
    Keep responses short (max 3 paragraphs). Always respond in $language.
    """
      .trimIndent()

  internal fun difficultyToRarity(difficulty: Int): String =
    when (difficulty) {
      1 -> "very common, everyday word"
      2 -> "common word"
      3 -> "less frequent word"
      4 -> "uncommon word"
      5 -> "rare or technical word"
      else -> "common word"
    }
}
