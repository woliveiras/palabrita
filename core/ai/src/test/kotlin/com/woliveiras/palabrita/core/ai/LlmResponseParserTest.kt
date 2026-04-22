package com.woliveiras.palabrita.core.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LlmResponseParserTest {

  private val parser = LlmResponseParserImpl()

  // --- Valid JSON ---

  @Test
  fun `parses valid JSON response`() {
    val json =
      """
      {"word":"gatos","category":"animal","difficulty":2,"hints":["Dica 1","Dica 2","Dica 3","Dica 4","Dica 5"]}
    """
        .trimIndent()

    val result = parser.parsePuzzle(json)

    assertThat(result).isInstanceOf(ParseResult.Success::class.java)
    val puzzle = (result as ParseResult.Success).data
    assertThat(puzzle.word).isEqualTo("gatos")
    assertThat(puzzle.category).isEqualTo("animal")
    assertThat(puzzle.difficulty).isEqualTo(2)
    assertThat(puzzle.hints).hasSize(5)
  }

  @Test
  fun `parses pretty-printed JSON`() {
    val json =
      """
      {
        "word": "campo",
        "category": "lugar",
        "difficulty": 1,
        "hints": [
          "É aberto",
          "Tem grama",
          "Animais pastam",
          "Fica fora da cidade",
          "Zona rural"
        ]
      }
    """
        .trimIndent()

    val result = parser.parsePuzzle(json)

    assertThat(result).isInstanceOf(ParseResult.Success::class.java)
    assertThat((result as ParseResult.Success).data.word).isEqualTo("campo")
  }

  // --- JSON extraction from noisy response ---

  @Test
  fun `extracts JSON from response with surrounding text`() {
    val raw =
      """
      Here is the word for the game:
      {"word":"gatos","category":"animal","difficulty":2,"hints":["A","B","C","D","E"]}
      I hope this helps!
    """
        .trimIndent()

    val result = parser.parsePuzzle(raw)

    assertThat(result).isInstanceOf(ParseResult.Success::class.java)
    assertThat((result as ParseResult.Success).data.word).isEqualTo("gatos")
  }

  @Test
  fun `extracts JSON from response with markdown code blocks`() {
    val raw =
      """
      ```json
      {"word":"gatos","category":"animal","difficulty":2,"hints":["A","B","C","D","E"]}
      ```
    """
        .trimIndent()

    val result = parser.parsePuzzle(raw)

    assertThat(result).isInstanceOf(ParseResult.Success::class.java)
    assertThat((result as ParseResult.Success).data.word).isEqualTo("gatos")
  }

  // --- Invalid responses ---

  @Test
  fun `returns error for completely invalid text`() {
    val raw = "This is not JSON at all"

    val result = parser.parsePuzzle(raw)

    assertThat(result).isInstanceOf(ParseResult.Error::class.java)
    assertThat((result as ParseResult.Error).rawResponse).isEqualTo(raw)
  }

  @Test
  fun `returns error for empty string`() {
    val result = parser.parsePuzzle("")

    assertThat(result).isInstanceOf(ParseResult.Error::class.java)
  }

  @Test
  fun `returns error for incomplete JSON`() {
    val raw = """{"word":"gatos","category":"""

    val result = parser.parsePuzzle(raw)

    assertThat(result).isInstanceOf(ParseResult.Error::class.java)
  }

  @Test
  fun `returns error for JSON missing required fields`() {
    val raw = """{"word":"gatos"}"""

    val result = parser.parsePuzzle(raw)

    assertThat(result).isInstanceOf(ParseResult.Error::class.java)
  }

  @Test
  fun `handles JSON with extra fields gracefully`() {
    val json =
      """
      {"word":"gatos","category":"animal","difficulty":2,"hints":["A","B","C","D","E"],"extra":"ignored"}
    """
        .trimIndent()

    val result = parser.parsePuzzle(json)

    assertThat(result).isInstanceOf(ParseResult.Success::class.java)
  }
}
