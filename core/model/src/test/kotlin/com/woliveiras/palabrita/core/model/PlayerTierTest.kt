package com.woliveiras.palabrita.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayerTierTest {

  @Test
  fun `fromXp returns NOVATO for zero xp`() {
    assertThat(PlayerTier.fromXp(0)).isEqualTo(PlayerTier.NOVATO)
  }

  @Test
  fun `fromXp returns NOVATO just below CURIOSO threshold`() {
    assertThat(PlayerTier.fromXp(49)).isEqualTo(PlayerTier.NOVATO)
  }

  @Test
  fun `fromXp returns CURIOSO at exact threshold`() {
    assertThat(PlayerTier.fromXp(50)).isEqualTo(PlayerTier.CURIOSO)
  }

  @Test
  fun `fromXp returns CURIOSO just below ASTUTO threshold`() {
    assertThat(PlayerTier.fromXp(149)).isEqualTo(PlayerTier.CURIOSO)
  }

  @Test
  fun `fromXp returns ASTUTO at exact threshold`() {
    assertThat(PlayerTier.fromXp(150)).isEqualTo(PlayerTier.ASTUTO)
  }

  @Test
  fun `fromXp returns SABIO at exact threshold`() {
    assertThat(PlayerTier.fromXp(400)).isEqualTo(PlayerTier.SABIO)
  }

  @Test
  fun `fromXp returns EPICO at exact threshold`() {
    assertThat(PlayerTier.fromXp(1000)).isEqualTo(PlayerTier.EPICO)
  }

  @Test
  fun `fromXp returns LENDARIO at exact threshold`() {
    assertThat(PlayerTier.fromXp(2500)).isEqualTo(PlayerTier.LENDARIO)
  }

  @Test
  fun `fromXp returns LENDARIO for very high xp`() {
    assertThat(PlayerTier.fromXp(10_000)).isEqualTo(PlayerTier.LENDARIO)
  }

  @Test
  fun `tier minXp values are in ascending order`() {
    val xpValues = PlayerTier.entries.map { it.minXp }
    assertThat(xpValues).isInOrder()
    // Verify strictly increasing (no duplicates)
    assertThat(xpValues.distinct()).hasSize(xpValues.size)
  }
}
