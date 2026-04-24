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
      {"word":"gatos","hints":["Dica 1","Dica 2","Dica 3"]}
      """
        .trimIndent()

    val result = parser.parsePuzzle(json)

    assertThat(result).isInstanceOf(ParseResult.Success::class.java)
    val puzzle = (result as ParseResult.Success).data
    assertThat(puzzle.word).isEqualTo("gatos")
    assertThat(puzzle.hints).hasSize(3)
  }

  @Test
  fun `parses pretty-printed JSON`() {
    val json =
      """
      {
        "word": "campo",
        "hints": [
          "É aberto",
          "Tem grama",
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
      {"word":"gatos","hints":["A","B","C"]}
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
      {"word":"gatos","hints":["A","B","C"]}
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
    val raw = """{"word":"gatos","hints":"""

    val result = parser.parsePuzzle(raw)

    assertThat(result).isInstanceOf(ParseResult.Error::class.java)
  }

  @Test
  fun `returns error for JSON missing hints`() {
    val raw = """{"word":"gatos"}"""

    val result = parser.parsePuzzle(raw)

    assertThat(result).isInstanceOf(ParseResult.Error::class.java)
  }

  @Test
  fun `handles JSON with extra fields gracefully`() {
    val json =
      """
      {"word":"gatos","category":"animal","difficulty":2,"hints":["A","B","C"],"extra":"ignored"}
      """
        .trimIndent()

    val result = parser.parsePuzzle(json)

    assertThat(result).isInstanceOf(ParseResult.Success::class.java)
  }

  // --- Mojibake / UTF-8 recovery ---

  @Test
  fun `strips replacement characters from mojibake input`() {
    val replacementChar = '\uFFFD'
    val json = """{"word":"a${replacementChar}o","hints":["Dica 1","Dica 2","Dica 3"]}"""

    val result = parser.parsePuzzle(json)

    assertThat(result).isInstanceOf(ParseResult.Success::class.java)
    val puzzle = (result as ParseResult.Success).data
    assertThat(puzzle.word).isEqualTo("ao")
  }
}
