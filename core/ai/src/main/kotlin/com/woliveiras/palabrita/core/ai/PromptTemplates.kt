package com.woliveiras.palabrita.core.ai

object PromptTemplates {

  fun puzzleSystemPrompt(): String =
    """
    You are a word generator for a guessing game. Your function is to return exactly one JSON object per request.
    Always respond with ONLY a JSON object. No markdown, no code fences, no explanation.
    The JSON keys MUST be in English: "word", "hints".
    The values for word and hints must be in the requested language.
    """
      .trimIndent()

  fun puzzleUserPromptLarge(language: String, wordLength: Int, recentWords: List<String>): String =
    """
      Generate a word for the game. Return ONLY a JSON object, no markdown.

      Required JSON format (keys MUST be in English):
      {"word": "string", "hints": ["h1","h2","h3"]}

      CRITICAL: The word MUST have exactly $wordLength letters. Count the letters carefully. Words with fewer or more letters will be rejected.

      Rules:
      - Output language for values: $language
      - The word MUST be a common noun in $language, exactly $wordLength lowercase letters, no accents
      - Exactly 3 progressive hints in $language: from vaguest to most specific
      - Hints MUST NOT contain the word itself
      - Avoid these recent words: ${recentWords.joinToString(", ")}
    """
      .trimIndent()

  fun puzzlePromptCompact(language: String, wordLength: Int, recentWords: List<String>): String =
    """
      You are a word generator for a game. Return ONLY a JSON object, no markdown, no code fences.

      Required JSON format (keys MUST be in English exactly as shown):
      {"word": "string", "hints": ["h1","h2","h3"]}

      CRITICAL RULE 1 — Word length: the word MUST have exactly $wordLength letters.
      Count every letter before answering.
      CRITICAL RULE 2 — Hints: NEVER include the word itself inside any hint.
      If the word is "gato", hints cannot contain "gato", "gatos", "gatito" or any form of it.

      Other rules:
      - The word must be a common noun in $language, lowercase, no accents
      - No proper nouns
      - Exactly 3 progressive hints in $language: from vaguest to most specific
      - Avoid these recent words: ${recentWords.joinToString(", ")}
    """
      .trimIndent()

  fun chatSystemPrompt(word: String, language: String): String =
    """
    You are an educational assistant. The player just guessed the word "$word".
    Answer questions about: word origin, etymology, fun facts, usage in sentences, synonyms, translations to other languages.
    Keep responses short (max 3 paragraphs). Always respond in $language.
    """
      .trimIndent()
}
