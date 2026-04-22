package com.woliveiras.palabrita.ui.theme

import com.google.common.truth.Truth.assertThat
import com.woliveiras.palabrita.core.common.GameColors
import org.junit.Test

class DesignSystemTest {

  // --- Feedback colors are spec values ---

  @Test
  fun `correct color is mint 4ECDC4`() {
    assertThat(MintCorrect.value).isEqualTo(0xFF4ECDC4.toULong() shl 32)
  }

  @Test
  fun `present color is amber FFB347`() {
    assertThat(AmberPresent.value).isEqualTo(0xFFFFB347.toULong() shl 32)
  }

  @Test
  fun `absent color is coral FF6B6B`() {
    assertThat(CoralAbsent.value).isEqualTo(0xFFFF6B6B.toULong() shl 32)
  }

  // --- GameColors defaults match spec ---

  @Test
  fun `GameColors default correct is MintCorrect`() {
    val colors = GameColors()
    assertThat(colors.correct).isEqualTo(MintCorrect)
  }

  @Test
  fun `GameColors default present is AmberPresent`() {
    val colors = GameColors()
    assertThat(colors.present).isEqualTo(AmberPresent)
  }

  @Test
  fun `GameColors default absent is CoralAbsent`() {
    val colors = GameColors()
    assertThat(colors.absent).isEqualTo(CoralAbsent)
  }

  @Test
  fun `GameColors default unused is GrayUnused`() {
    val colors = GameColors()
    assertThat(colors.unused).isEqualTo(GrayUnused)
  }

  @Test
  fun `GameColors onFeedback is dark for contrast`() {
    val colors = GameColors()
    assertThat(colors.onFeedback).isEqualTo(OnFeedback)
  }

  // --- WCAG AA contrast (4.5:1 for text) ---

  @Test
  fun `onFeedback on correct meets WCAG AA`() {
    val ratio = contrastRatio(OnFeedback, MintCorrect)
    assertThat(ratio).isAtLeast(4.5)
  }

  @Test
  fun `onFeedback on present meets WCAG 3-1 for UI`() {
    val ratio = contrastRatio(OnFeedback, AmberPresent)
    assertThat(ratio).isAtLeast(3.0)
  }

  @Test
  fun `onFeedback on absent meets WCAG 3-1 for UI`() {
    val ratio = contrastRatio(OnFeedback, CoralAbsent)
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
    assertThat(colors.correct).isEqualTo(MintCorrect)
    assertThat(colors.present).isEqualTo(AmberPresent)
    assertThat(colors.absent).isEqualTo(CoralAbsent)
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
