package com.woliveiras.palabrita.core.data.mapper

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.data.db.entity.PlayerStatsEntity
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.PlayerTier
import org.junit.Test

class PlayerStatsMapperTest {

  @Test
  fun `entity to domain maps basic fields`() {
    val entity =
      PlayerStatsEntity(
        totalPlayed = 50,
        totalWon = 35,
        currentStreak = 7,
        maxStreak = 12,
        avgAttempts = 3.5f,
        totalXp = 200,
        playerTier = "ASTUTO",
      )

    val domain = entity.toDomain()

    assertThat(domain.totalPlayed).isEqualTo(50)
    assertThat(domain.totalWon).isEqualTo(35)
    assertThat(domain.currentStreak).isEqualTo(7)
    assertThat(domain.playerTier).isEqualTo(PlayerTier.ASTUTO)
    assertThat(domain.totalXp).isEqualTo(200)
  }

  @Test
  fun `entity with JSON maps to domain maps`() {
    val entity =
      PlayerStatsEntity(
        gamesWonByDifficulty = """{"1":45,"2":30,"3":12}""",
        winRateByDifficulty = """{"1":0.85,"2":0.72,"3":0.60}""",
        guessDistribution = """{"1":5,"2":12,"3":18,"4":8,"5":3,"6":1}""",
      )

    val domain = entity.toDomain()

    assertThat(domain.gamesWonByDifficulty).containsEntry(1, 45)
    assertThat(domain.gamesWonByDifficulty).containsEntry(2, 30)
    assertThat(domain.winRateByDifficulty[1]).isWithin(0.01f).of(0.85f)
    assertThat(domain.guessDistribution).containsEntry(3, 18)
    assertThat(domain.guessDistribution).hasSize(6)
  }

  @Test
  fun `empty JSON maps to empty maps`() {
    val entity = PlayerStatsEntity(gamesWonByDifficulty = "{}", guessDistribution = "{}")

    val domain = entity.toDomain()

    assertThat(domain.gamesWonByDifficulty).isEmpty()
    assertThat(domain.guessDistribution).isEmpty()
  }

  @Test
  fun `domain to entity serializes maps to JSON`() {
    val domain =
      PlayerStats(
        gamesWonByDifficulty = mapOf(1 to 10, 2 to 5),
        guessDistribution = mapOf(1 to 3, 2 to 7),
      )

    val entity = domain.toEntity()

    assertThat(entity.gamesWonByDifficulty).contains("1")
    assertThat(entity.gamesWonByDifficulty).contains("10")
    assertThat(entity.guessDistribution).contains("2")
    assertThat(entity.guessDistribution).contains("7")
  }

  @Test
  fun `roundtrip preserves all data`() {
    val original =
      PlayerStats(
        totalPlayed = 100,
        totalWon = 75,
        currentStreak = 15,
        maxStreak = 30,
        avgAttempts = 3.2f,
        totalXp = 500,
        playerTier = PlayerTier.SABIO,
        gamesWonByDifficulty = mapOf(1 to 40, 2 to 25, 3 to 10),
        winRateByDifficulty = mapOf(1 to 0.9f, 2 to 0.75f),
        guessDistribution = mapOf(1 to 5, 2 to 10, 3 to 20, 4 to 15, 5 to 10, 6 to 5),
      )

    val roundtripped = original.toEntity().toDomain()

    assertThat(roundtripped.totalPlayed).isEqualTo(100)
    assertThat(roundtripped.playerTier).isEqualTo(PlayerTier.SABIO)
    assertThat(roundtripped.gamesWonByDifficulty).isEqualTo(original.gamesWonByDifficulty)
    assertThat(roundtripped.guessDistribution).isEqualTo(original.guessDistribution)
  }

  @Test
  fun `parseIntMap handles empty string`() {
    assertThat(parseIntMap("")).isEmpty()
    assertThat(parseIntMap("{}")).isEmpty()
  }

  @Test
  fun `parseFloatMap handles empty string`() {
    assertThat(parseFloatMap("")).isEmpty()
    assertThat(parseFloatMap("{}")).isEmpty()
  }
}
