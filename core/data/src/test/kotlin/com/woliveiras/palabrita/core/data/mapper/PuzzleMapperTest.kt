package com.woliveiras.palabrita.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.data.db.entity.PuzzleEntity
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.PuzzleSource
import org.junit.Test

class PuzzleMapperTest {

  @Test
  fun `entity to domain maps all fields`() {
    val entity =
      PuzzleEntity(
        id = 1,
        word = "gatos",
        wordDisplay = "gatos",
        language = "pt",
        difficulty = 2,
        category = "animal",
        hints = """["Dica 1","Dica 2","Dica 3","Dica 4","Dica 5"]""",
        source = "AI",
        generatedAt = 1000L,
        playedAt = 2000L,
        isPlayed = true,
        isValid = true,
      )

    val domain = entity.toDomain()

    assertThat(domain.id).isEqualTo(1)
    assertThat(domain.word).isEqualTo("gatos")
    assertThat(domain.hints).containsExactly("Dica 1", "Dica 2", "Dica 3", "Dica 4", "Dica 5")
    assertThat(domain.source).isEqualTo(PuzzleSource.AI)
    assertThat(domain.playedAt).isEqualTo(2000L)
    assertThat(domain.isPlayed).isTrue()
  }

  @Test
  fun `domain to entity maps all fields`() {
    val domain =
      Puzzle(
        id = 1,
        word = "gatos",
        wordDisplay = "gatos",
        language = "pt",
        difficulty = 2,
        category = "animal",
        hints = listOf("Dica 1", "Dica 2", "Dica 3", "Dica 4", "Dica 5"),
        source = PuzzleSource.AI,
        generatedAt = 1000L,
      )

    val entity = domain.toEntity()

    assertThat(entity.word).isEqualTo("gatos")
    assertThat(entity.source).isEqualTo("AI")
    assertThat(entity.hints).contains("Dica 1")
    assertThat(entity.hints).contains("Dica 5")
  }

  @Test
  fun `hints roundtrip preserves data`() {
    val hints = listOf("Tem quatro patas", "É doméstico", "Ronrona", "Persegue ratos", "Mia")
    val domain =
      Puzzle(
        word = "gatos",
        wordDisplay = "gatos",
        language = "pt",
        difficulty = 2,
        category = "animal",
        hints = hints,
        source = PuzzleSource.AI,
        generatedAt = 1000L,
      )

    val roundtripped = domain.toEntity().toDomain()

    assertThat(roundtripped.hints).isEqualTo(hints)
  }

  @Test
  fun `source AI roundtrips correctly`() {
    val entity =
      PuzzleEntity(
        word = "gatos", wordDisplay = "gatos", language = "pt", difficulty = 2,
        category = "animal", hints = "[]", source = "AI", generatedAt = 0,
      )

    assertThat(entity.toDomain().toEntity().source).isEqualTo("AI")
  }
}
