package com.woliveiras.palabrita.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.data.db.entity.PlayerStatsEntity
import com.woliveiras.palabrita.core.model.PlayerStats
import org.junit.Test

class PlayerStatsMapperTest {

  @Test
  fun `entity to domain maps basic fields`() {
    val entity = PlayerStatsEntity(totalPlayed = 50, totalWon = 35, avgAttempts = 3.5f)

    val domain = entity.toDomain()

    assertThat(domain.totalPlayed).isEqualTo(50)
    assertThat(domain.totalWon).isEqualTo(35)
    assertThat(domain.avgAttempts).isWithin(0.01f).of(3.5f)
  }

  @Test
  fun `entity with JSON maps to domain maps`() {
    val entity =
      PlayerStatsEntity(guessDistribution = """{"1":5,"2":12,"3":18,"4":8,"5":3,"6":1}""")

    val domain = entity.toDomain()

    assertThat(domain.guessDistribution).containsEntry(3, 18)
    assertThat(domain.guessDistribution).hasSize(6)
  }

  @Test
  fun `empty JSON maps to empty maps`() {
    val entity = PlayerStatsEntity(guessDistribution = "{}")

    val domain = entity.toDomain()

    assertThat(domain.guessDistribution).isEmpty()
  }

  @Test
  fun `domain to entity serializes maps to JSON`() {
    val domain = PlayerStats(guessDistribution = mapOf(1 to 3, 2 to 7))

    val entity = domain.toEntity()

    assertThat(entity.guessDistribution).contains("1")
    assertThat(entity.guessDistribution).contains("3")
    assertThat(entity.guessDistribution).contains("2")
    assertThat(entity.guessDistribution).contains("7")
  }

  @Test
  fun `roundtrip preserves all data`() {
    val original =
      PlayerStats(
        totalPlayed = 100,
        totalWon = 75,
        avgAttempts = 3.2f,
        guessDistribution = mapOf(1 to 5, 2 to 10, 3 to 20, 4 to 15, 5 to 10, 6 to 5),
      )

    val roundtripped = original.toEntity().toDomain()

    assertThat(roundtripped.totalPlayed).isEqualTo(100)
    assertThat(roundtripped.guessDistribution).isEqualTo(original.guessDistribution)
  }

  @Test
  fun `parseIntMap handles empty string`() {
    assertThat(parseIntMap("")).isEmpty()
    assertThat(parseIntMap("{}")).isEmpty()
  }
}
