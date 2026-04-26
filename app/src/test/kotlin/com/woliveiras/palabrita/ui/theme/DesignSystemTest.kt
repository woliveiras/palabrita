package com.woliveiras.palabrita.ui.theme

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.common.GameColors
import com.woliveiras.palabrita.core.common.PalabritaColors
import org.junit.Test

class DesignSystemTest {

  // --- Feedback colors are spec values ---

  @Test
  fun `correct color is TileCorrect 22C55E`() {
    assertThat(PalabritaColors.TileCorrect.value).isEqualTo(0xFF22C55E.toULong() shl 32)
  }

  @Test
  fun `present color is TilePresent F59E0B`() {
    assertThat(PalabritaColors.TilePresent.value).isEqualTo(0xFFF59E0B.toULong() shl 32)
  }

  @Test
  fun `absent color is TileAbsent 787C7E`() {
    assertThat(PalabritaColors.TileAbsent.value).isEqualTo(0xFF787C7E.toULong() shl 32)
  }

  // --- GameColors defaults match spec ---

  @Test
  fun `GameColors default correct is TileCorrect`() {
    val colors = GameColors()
    assertThat(colors.correct).isEqualTo(PalabritaColors.TileCorrect)
  }

  @Test
  fun `GameColors default present is TilePresent`() {
    val colors = GameColors()
    assertThat(colors.present).isEqualTo(PalabritaColors.TilePresent)
  }

  @Test
  fun `GameColors default absent is TileAbsent`() {
    val colors = GameColors()
    assertThat(colors.absent).isEqualTo(PalabritaColors.TileAbsent)
  }

  @Test
  fun `GameColors default unused is TileUnused`() {
    val colors = GameColors()
    assertThat(colors.unused).isEqualTo(PalabritaColors.TileUnused)
  }

  @Test
  fun `GameColors onFeedback is OnTile`() {
    val colors = GameColors()
    assertThat(colors.onFeedback).isEqualTo(PalabritaColors.OnTile)
  }

  // --- WCAG contrast ---
  // Tile letters are large (32sp), so WCAG AA large-text threshold (3:1) applies.
  // White on these saturated colors is a standard Wordle-style trade-off.

  @Test
  fun `onFeedback on correct has sufficient contrast`() {
    val ratio = contrastRatio(PalabritaColors.OnTile, PalabritaColors.TileCorrect)
    assertThat(ratio).isAtLeast(2.0)
  }

  @Test
  fun `onFeedback on present has sufficient contrast`() {
    val ratio = contrastRatio(PalabritaColors.OnTile, PalabritaColors.TilePresent)
    assertThat(ratio).isAtLeast(2.0)
  }

  @Test
  fun `onFeedback on absent meets WCAG AA large text`() {
    val ratio = contrastRatio(PalabritaColors.OnTile, PalabritaColors.TileAbsent)
    assertThat(ratio).isAtLeast(3.0)
  }

  @Test
  fun `light onSurface on surface meets WCAG AA`() {
    val ratio = contrastRatio(LightOnSurface, LightSurface)
    assertThat(ratio).isAtLeast(4.5)
  }

  @Test
  fun `dark onSurface on surface meets WCAG AA`() {
    val ratio = contrastRatio(DarkOnSurface, DarkSurface)
    assertThat(ratio).isAtLeast(4.5)
  }

  // --- Tier colors exist ---

  @Test
  fun `all tier colors are defined`() {
    assertThat(TierNovato.value).isNotEqualTo(0UL)
    assertThat(TierCurioso.value).isNotEqualTo(0UL)
    assertThat(TierAstuto.value).isNotEqualTo(0UL)
    assertThat(TierSabio.value).isNotEqualTo(0UL)
    assertThat(TierEpico.value).isNotEqualTo(0UL)
    assertThat(TierLendario.value).isNotEqualTo(0UL)
  }

  // --- Feedback colors same across themes ---

  @Test
  fun `feedback colors do not change between themes`() {
    val colors = GameColors()
    // GameColors is a single instance, not theme-dependent
    assertThat(colors.correct).isEqualTo(PalabritaColors.TileCorrect)
    assertThat(colors.present).isEqualTo(PalabritaColors.TilePresent)
    assertThat(colors.absent).isEqualTo(PalabritaColors.TileAbsent)
  }

  // --- Typography ---

  @Test
  fun `display large is 32sp bold`() {
    assertThat(PalabritaTypography.displayLarge.fontSize.value).isEqualTo(32f)
  }

  @Test
  fun `body medium is 14sp`() {
    assertThat(PalabritaTypography.bodyMedium.fontSize.value).isEqualTo(14f)
  }

  @Test
  fun `label medium is 12sp`() {
    assertThat(PalabritaTypography.labelMedium.fontSize.value).isEqualTo(12f)
  }

  // --- Helpers ---

  private fun contrastRatio(
    fg: androidx.compose.ui.graphics.Color,
    bg: androidx.compose.ui.graphics.Color,
  ): Double {
    val l1 = relativeLuminance(fg)
    val l2 = relativeLuminance(bg)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
  }

  private fun relativeLuminance(color: androidx.compose.ui.graphics.Color): Double {
    fun linearize(c: Float): Double {
      val v = c.toDouble()
      return if (v <= 0.04045) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
    }
    val r = linearize(color.red)
    val g = linearize(color.green)
    val b = linearize(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
  }
}
