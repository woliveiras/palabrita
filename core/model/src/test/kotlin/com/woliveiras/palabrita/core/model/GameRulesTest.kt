package com.woliveiras.palabrita.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameRulesTest {

  @Test
  fun `levelForCycle returns first level for cycle 0`() {
    val (wordLength, batchSize) = GameRules.levelForCycle(0)
    assertThat(wordLength).isEqualTo(4)
    assertThat(batchSize).isEqualTo(5)
  }

  @Test
  fun `levelForCycle returns second level for cycle 1`() {
    val (wordLength, batchSize) = GameRules.levelForCycle(1)
    assertThat(wordLength).isEqualTo(5)
    assertThat(batchSize).isEqualTo(10)
  }

  @Test
  fun `levelForCycle returns last level for cycle 2 and beyond`() {
    val (wl2, bs2) = GameRules.levelForCycle(2)
    assertThat(wl2).isEqualTo(6)
    assertThat(bs2).isEqualTo(10)

    val (wl99, bs99) = GameRules.levelForCycle(99)
    assertThat(wl99).isEqualTo(6)
    assertThat(bs99).isEqualTo(10)
  }

  @Test
  fun `levelForCycle clamps negative cycles to 0`() {
    val (wordLength, batchSize) = GameRules.levelForCycle(-1)
    assertThat(wordLength).isEqualTo(4)
    assertThat(batchSize).isEqualTo(5)
  }
}
