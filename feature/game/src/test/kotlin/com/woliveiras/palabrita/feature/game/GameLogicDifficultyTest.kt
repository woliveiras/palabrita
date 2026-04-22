package com.woliveiras.palabrita.feature.game

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameLogicDifficultyTest {

  // --- difficultyToWordLength ---

  @Test
  fun `difficulty 1 returns 5-5 for default preference`() {
    assertThat(GameLogic.difficultyToWordLength(1, "DEFAULT")).isEqualTo(5..5)
  }

  @Test
  fun `difficulty 2 returns 5-6 for default preference`() {
    assertThat(GameLogic.difficultyToWordLength(2, "DEFAULT")).isEqualTo(5..6)
  }

  @Test
  fun `difficulty 3 returns 6-7 for default preference`() {
    assertThat(GameLogic.difficultyToWordLength(3, "DEFAULT")).isEqualTo(6..7)
  }

  @Test
  fun `difficulty 4 returns 7-8 for default preference`() {
    assertThat(GameLogic.difficultyToWordLength(4, "DEFAULT")).isEqualTo(7..8)
  }

  @Test
  fun `difficulty 5 returns 7-8 for default preference`() {
    assertThat(GameLogic.difficultyToWordLength(5, "DEFAULT")).isEqualTo(7..8)
  }

  @Test
  fun `SHORT preference overrides difficulty`() {
    assertThat(GameLogic.difficultyToWordLength(5, "SHORT")).isEqualTo(5..6)
  }

  @Test
  fun `LONG preference overrides difficulty`() {
    assertThat(GameLogic.difficultyToWordLength(1, "LONG")).isEqualTo(7..9)
  }

  @Test
  fun `EPIC preference overrides difficulty`() {
    assertThat(GameLogic.difficultyToWordLength(1, "EPIC")).isEqualTo(8..10)
  }

  @Test
  fun `unknown difficulty falls back to 5-6`() {
    assertThat(GameLogic.difficultyToWordLength(99, "DEFAULT")).isEqualTo(5..6)
  }
}
