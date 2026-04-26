package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PromptTemplatesTest {

  @Test
  fun `hint system prompt contains rules`() {
    val prompt = PromptTemplates.hintSystemPrompt("pt")
    assertThat(prompt).contains("hints")
    assertThat(prompt).contains("3")
  }

  @Test
  fun `hint system prompt does not ask for word generation`() {
    val prompt = PromptTemplates.hintSystemPrompt("en")
    assertThat(prompt).doesNotContain("generate a word")
    assertThat(prompt).doesNotContain("JSON")
  }

  @Test
  fun `hint user prompt includes target word`() {
    val prompt = PromptTemplates.hintUserPrompt(word = "mesa", language = "pt")
    assertThat(prompt).contains("MESA")
  }

  @Test
  fun `hint user prompt includes language`() {
    val prompt = PromptTemplates.hintUserPrompt(word = "table", language = "en")
    assertThat(prompt).contains("English")
  }

  @Test
  fun `hint user prompt includes bad hint examples`() {
    val prompt = PromptTemplates.hintUserPrompt(word = "mesa", language = "pt")
    assertThat(prompt).contains("Bad hints")
  }

  @Test
  fun `chat system prompt includes word`() {
    val prompt = PromptTemplates.chatSystemPrompt(word = "gatos", language = "pt")
    assertThat(prompt).contains("gatos")
  }

  @Test
  fun `chat system prompt includes language display name`() {
    val prompt = PromptTemplates.chatSystemPrompt(word = "cats", language = "en")
    assertThat(prompt).contains("English")
  }

  @Test
  fun `chat system prompt is in english`() {
    val prompt = PromptTemplates.chatSystemPrompt(word = "gatos", language = "pt")
    assertThat(prompt).contains("educational assistant")
  }

  // --- Language display name mapping ---

  @Test
  fun `languageDisplayName maps pt to Brazilian Portuguese`() {
    assertThat(PromptTemplates.languageDisplayName("pt")).isEqualTo("Brazilian Portuguese")
  }

  @Test
  fun `languageDisplayName maps en to English`() {
    assertThat(PromptTemplates.languageDisplayName("en")).isEqualTo("English")
  }

  @Test
  fun `languageDisplayName maps es to Spanish`() {
    assertThat(PromptTemplates.languageDisplayName("es")).isEqualTo("Spanish")
  }

  @Test
  fun `languageDisplayName falls back to raw code for unknown language`() {
    assertThat(PromptTemplates.languageDisplayName("fr")).isEqualTo("fr")
  }

  @Test
  fun `hint system prompt enforces language for pt`() {
    val prompt = PromptTemplates.hintSystemPrompt("pt")
    assertThat(prompt).contains("Brazilian Portuguese")
    assertThat(prompt).contains("MUST be written in Brazilian Portuguese")
  }

  @Test
  fun `hint system prompt enforces language for es`() {
    val prompt = PromptTemplates.hintSystemPrompt("es")
    assertThat(prompt).contains("Spanish")
    assertThat(prompt).contains("MUST be written in Spanish")
  }

  @Test
  fun `hint user prompt enforces language for pt`() {
    val prompt = PromptTemplates.hintUserPrompt(word = "mesa", language = "pt")
    assertThat(prompt).contains("IN Brazilian Portuguese")
  }

  @Test
  fun `hint user prompt uses Brazilian Portuguese for pt`() {
    val prompt = PromptTemplates.hintUserPrompt(word = "mesa", language = "pt")
    assertThat(prompt).contains("Brazilian Portuguese")
  }

  @Test
  fun `hint user prompt uses Spanish for es`() {
    val prompt = PromptTemplates.hintUserPrompt(word = "mesa", language = "es")
    assertThat(prompt).contains("Spanish")
  }

  @Test
  fun `chat system prompt uses Brazilian Portuguese for pt`() {
    val prompt = PromptTemplates.chatSystemPrompt(word = "gatos", language = "pt")
    assertThat(prompt).contains("Brazilian Portuguese")
  }
}
