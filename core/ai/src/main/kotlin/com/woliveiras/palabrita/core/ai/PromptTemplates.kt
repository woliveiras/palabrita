package com.woliveiras.palabrita.core.ai

object PromptTemplates {

  fun puzzleSystemPromptGemma4(): String =
    """
    You are a word generator for a guessing game.
    Always respond using the provided function. Never add text outside the function call.
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
      Generate a word for the game.
      Output language: $language
      Difficulty: $difficulty (1=easy, 5=hard)
      Length: $minLength-$maxLength letters
      Word rarity: $rarity
      The word, category, and hints MUST be in $language.
      Avoid these recent words: ${recentWords.joinToString(", ")}
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
      You are a word generator for a game. Return ONLY valid JSON, no extra text.

      Schema:
      {"word": "string", "category": "string", "difficulty": number, "hints": ["string","string","string","string","string"]}

      Rules:
      - The word MUST be a common noun in $language, $minLength-$maxLength letters
      - Word rarity: $rarity
      - No proper nouns, no accents, lowercase only
      - difficulty: $difficulty (1=easy, 5=hard)
      - 5 progressive hints: from vaguest to most specific, written in $language
      - Hints MUST NOT contain the word
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
