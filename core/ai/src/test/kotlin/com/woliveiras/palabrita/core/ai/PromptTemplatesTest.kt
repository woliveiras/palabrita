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
  fun `puzzle system prompt does not mention category or difficulty`() {
    val prompt = PromptTemplates.puzzleSystemPrompt()
    assertThat(prompt).doesNotContain("category")
    assertThat(prompt).doesNotContain("difficulty")
  }

  @Test
  fun `puzzle user prompt large includes language parameter`() {
    val prompt =
      PromptTemplates.puzzleUserPromptLarge(
        language = "pt",
        wordLength = 6,
        recentWords = emptyList(),
      )
    assertThat(prompt).contains("pt")
    assertThat(prompt).contains("6 letters")
  }

  @Test
  fun `puzzle user prompt large includes recent words`() {
    val prompt =
      PromptTemplates.puzzleUserPromptLarge(
        language = "pt",
        wordLength = 5,
        recentWords = listOf("gatos", "campo"),
      )
    assertThat(prompt).contains("gatos")
    assertThat(prompt).contains("campo")
  }

  @Test
  fun `puzzle user prompt large does not mention category or difficulty`() {
    val prompt =
      PromptTemplates.puzzleUserPromptLarge(
        language = "pt",
        wordLength = 5,
        recentWords = emptyList(),
      )
    assertThat(prompt).doesNotContain("category")
    assertThat(prompt).doesNotContain("difficulty")
  }

  @Test
  fun `puzzle prompt compact includes JSON schema`() {
    val prompt =
      PromptTemplates.puzzlePromptCompact(
        language = "en",
        wordLength = 5,
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
        wordLength = 5,
        recentWords = emptyList(),
      )
    assertThat(prompt).contains("es")
  }

  @Test
  fun `puzzle prompt compact asks for 3 hints`() {
    val prompt =
      PromptTemplates.puzzlePromptCompact(
        language = "pt",
        wordLength = 5,
        recentWords = emptyList(),
      )
    assertThat(prompt).contains("3 progressive hints")
  }

  @Test
  fun `chat system prompt includes word`() {
    val prompt = PromptTemplates.chatSystemPrompt(word = "gatos", language = "pt")
    assertThat(prompt).contains("gatos")
  }

  @Test
  fun `chat system prompt includes language`() {
    val prompt = PromptTemplates.chatSystemPrompt(word = "cats", language = "en")
    assertThat(prompt).contains("en")
  }

  @Test
  fun `chat system prompt is in english`() {
    val prompt = PromptTemplates.chatSystemPrompt(word = "gatos", language = "pt")
    assertThat(prompt).contains("educational assistant")
  }
}
