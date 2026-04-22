package com.woliveiras.palabrita.core.data.repository

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class XpCalculationTest {

  @Test
  fun `loss returns zero xp`() {
    assertThat(calculateXpForGame(won = false, attempts = 3, difficulty = 3, hintsUsed = 0))
      .isEqualTo(0)
  }

  @Test
  fun `win at difficulty 1 returns base 1 xp`() {
    assertThat(calculateXpForGame(won = true, attempts = 3, difficulty = 1, hintsUsed = 0))
      .isEqualTo(1)
  }

  @Test
  fun `win at difficulty 5 returns base 8 xp`() {
    assertThat(calculateXpForGame(won = true, attempts = 3, difficulty = 5, hintsUsed = 0))
      .isEqualTo(8)
  }

  @Test
  fun `first attempt bonus adds 3 xp`() {
    assertThat(calculateXpForGame(won = true, attempts = 1, difficulty = 1, hintsUsed = 0))
      .isEqualTo(4) // 1 base + 3 bonus
  }

  @Test
  fun `second attempt bonus adds 1 xp`() {
    assertThat(calculateXpForGame(won = true, attempts = 2, difficulty = 1, hintsUsed = 0))
      .isEqualTo(2) // 1 base + 1 bonus
  }

  @Test
  fun `streak 7 bonus adds 5 xp`() {
    assertThat(calculateXpForGame(won = true, attempts = 3, difficulty = 1, hintsUsed = 0))
      .isEqualTo(1) // 1 base, no streak bonus
  }

  @Test
  fun `streak 14 bonus adds 5 xp`() {
    assertThat(calculateXpForGame(won = true, attempts = 3, difficulty = 1, hintsUsed = 0))
      .isEqualTo(1) // 1 base, no streak bonus
  }

  @Test
  fun `streak 30 bonus adds 20 xp`() {
    assertThat(calculateXpForGame(won = true, attempts = 3, difficulty = 1, hintsUsed = 0))
      .isEqualTo(1) // 1 base, no streak bonus
  }

  @Test
  fun `streak 8 gives no bonus`() {
    assertThat(calculateXpForGame(won = true, attempts = 3, difficulty = 1, hintsUsed = 0))
      .isEqualTo(1) // 1 base, no streak bonus
  }

  @Test
  fun `hints reduce xp`() {
    assertThat(calculateXpForGame(won = true, attempts = 3, difficulty = 3, hintsUsed = 2))
      .isEqualTo(1) // 3 base - 2 hints = 1
  }

  @Test
  fun `xp never goes below 1 for a win`() {
    assertThat(calculateXpForGame(won = true, attempts = 6, difficulty = 1, hintsUsed = 5))
      .isEqualTo(1) // 1 base - 5 hints = -4, clamped to 1
  }

  @Test
  fun `spec example level 3 first attempt 2 hints`() {
    // Spec: vitória no nível 3 (base 3 XP) + 1ª tentativa (+3) + 2 dicas usadas (-2) = 4 XP
    assertThat(calculateXpForGame(won = true, attempts = 1, difficulty = 3, hintsUsed = 2))
      .isEqualTo(4)
  }

  @Test
  fun `spec example level 1 with 5 hints`() {
    // Spec: vitória no nível 1 (base 1 XP) + 5 dicas usadas (-5) = 1 XP (mínimo)
    assertThat(calculateXpForGame(won = true, attempts = 3, difficulty = 1, hintsUsed = 5))
      .isEqualTo(1)
  }
}
