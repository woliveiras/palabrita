package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PromptTemplatesTest {

  @Test
  fun `puzzle system prompt is in english`() {
    val prompt = PromptTemplates.puzzleSystemPrompt()
    assertThat(prompt).contains("word generator")
    assertThat(prompt).contains("function")
  }

  @Test
  fun `puzzle user prompt large includes language parameter`() {
    val prompt =
      PromptTemplates.puzzleUserPromptLarge(
        language = "pt",
        difficulty = 3,
        minLength = 6,
        maxLength = 7,
        recentWords = emptyList(),
      )
    assertThat(prompt).contains("pt")
    assertThat(prompt).contains("6-7")
  }

  @Test
  fun `puzzle user prompt large includes recent words`() {
    val prompt =
      PromptTemplates.puzzleUserPromptLarge(
        language = "pt",
        difficulty = 2,
        minLength = 5,
        maxLength = 6,
        recentWords = listOf("gatos", "campo"),
      )
    assertThat(prompt).contains("gatos")
    assertThat(prompt).contains("campo")
  }

  @Test
  fun `puzzle prompt compact includes JSON schema`() {
    val prompt =
      PromptTemplates.puzzlePromptCompact(
        language = "en",
        difficulty = 1,
        minLength = 5,
        maxLength = 5,
        recentWords = emptyList(),
      )
    assertThat(prompt).contains("\"word\"")
    assertThat(prompt).contains("\"hints\"")
    assertThat(prompt).contains("JSON")
  }

  @Test
  fun `puzzle prompt compact includes language output instruction`() {
    val prompt =
      PromptTemplates.puzzlePromptCompact(
        language = "es",
        difficulty = 2,
        minLength = 5,
        maxLength = 6,
        recentWords = emptyList(),
      )
    assertThat(prompt).contains("es")
  }

  @Test
  fun `difficulty 1 maps to everyday rarity`() {
    val prompt =
      PromptTemplates.puzzleUserPromptLarge(
        language = "pt",
        difficulty = 1,
        minLength = 5,
        maxLength = 5,
        recentWords = emptyList(),
      )
    assertThat(prompt).containsMatch("(?i)everyday|very common")
  }

  @Test
  fun `difficulty 5 maps to rare rarity`() {
    val prompt =
      PromptTemplates.puzzleUserPromptLarge(
        language = "pt",
        difficulty = 5,
        minLength = 7,
        maxLength = 8,
        recentWords = emptyList(),
      )
    assertThat(prompt).containsMatch("(?i)rare|technical")
  }

  @Test
  fun `chat system prompt includes word and category`() {
    val prompt =
      PromptTemplates.chatSystemPrompt(word = "gatos", category = "animal", language = "pt")
    assertThat(prompt).contains("gatos")
    assertThat(prompt).contains("animal")
  }

  @Test
  fun `chat system prompt includes language`() {
    val prompt =
      PromptTemplates.chatSystemPrompt(word = "cats", category = "animal", language = "en")
    assertThat(prompt).contains("en")
  }

  @Test
  fun `chat system prompt is in english`() {
    val prompt =
      PromptTemplates.chatSystemPrompt(word = "gatos", category = "animal", language = "pt")
    assertThat(prompt).contains("educational assistant")
  }
}
