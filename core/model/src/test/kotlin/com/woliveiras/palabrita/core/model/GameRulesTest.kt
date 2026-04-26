package com.woliveiras.palabrita.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameRulesTest {

  @Test
  fun `levelForCycle returns first level for cycle 0`() {
    val level = GameRules.levelForCycle(0)
    assertThat(level.wordLength).isEqualTo(4)
    assertThat(level.batchSize).isEqualTo(5)
  }

  @Test
  fun `levelForCycle returns second level for cycle 1`() {
    val level = GameRules.levelForCycle(1)
    assertThat(level.wordLength).isEqualTo(5)
    assertThat(level.batchSize).isEqualTo(10)
  }

  @Test
  fun `levelForCycle returns last level for cycle 2 and beyond`() {
    val l2 = GameRules.levelForCycle(2)
    assertThat(l2.wordLength).isEqualTo(6)
    assertThat(l2.batchSize).isEqualTo(10)

    val l99 = GameRules.levelForCycle(99)
    assertThat(l99.wordLength).isEqualTo(6)
    assertThat(l99.batchSize).isEqualTo(10)
  }

  @Test
  fun `levelForCycle clamps negative cycles to 0`() {
    val level = GameRules.levelForCycle(-1)
    assertThat(level.wordLength).isEqualTo(4)
    assertThat(level.batchSize).isEqualTo(5)
  }

  // --- winsRequired ---

  @Test
  fun `level 1 requires 5 wins`() {
    assertThat(GameRules.levelForCycle(0).winsRequired).isEqualTo(5)
  }

  @Test
  fun `level 2 requires 10 wins`() {
    assertThat(GameRules.levelForCycle(1).winsRequired).isEqualTo(10)
  }

  @Test
  fun `level 3 and beyond requires 10 wins`() {
    assertThat(GameRules.levelForCycle(2).winsRequired).isEqualTo(10)
    assertThat(GameRules.levelForCycle(5).winsRequired).isEqualTo(10)
  }
}
