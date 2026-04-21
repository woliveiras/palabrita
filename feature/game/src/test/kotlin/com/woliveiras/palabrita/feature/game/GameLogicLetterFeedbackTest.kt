package com.woliveiras.palabrita.feature.game

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameLogicLetterFeedbackTest {

  // --- Basic feedback ---

  @Test
  fun `all correct letters return CORRECT`() {
    val feedback = GameLogic.calculateLetterFeedback("gatos", "gatos")
    assertThat(feedback.map { it.state }).containsExactly(
      LetterState.CORRECT, LetterState.CORRECT, LetterState.CORRECT,
      LetterState.CORRECT, LetterState.CORRECT,
    )
  }

  @Test
  fun `all wrong letters return ABSENT`() {
    val feedback = GameLogic.calculateLetterFeedback("fuwxy", "gatos")
    assertThat(feedback.map { it.state }).containsExactly(
      LetterState.ABSENT, LetterState.ABSENT, LetterState.ABSENT,
      LetterState.ABSENT, LetterState.ABSENT,
    )
  }

  @Test
  fun `present letter in wrong position returns PRESENT`() {
    val feedback = GameLogic.calculateLetterFeedback("togsa", "gatos")
    // t: position 0, target has t at 2 → PRESENT
    // o: position 1, target has o at 3 → PRESENT
    // g: position 2, target has g at 0 → PRESENT
    // s: position 3, target has s at 4 → PRESENT
    // a: position 4, target has a at 1 → PRESENT
    assertThat(feedback.map { it.state }).containsExactly(
      LetterState.PRESENT, LetterState.PRESENT, LetterState.PRESENT,
      LetterState.PRESENT, LetterState.PRESENT,
    )
  }

  @Test
  fun `mix of correct present and absent`() {
    val feedback = GameLogic.calculateLetterFeedback("galho", "gatos")
    // g → CORRECT (pos 0)
    // a → CORRECT (pos 1)
    // l → ABSENT
    // h → ABSENT
    // o → PRESENT (exists at pos 3 in target)
    assertThat(feedback).containsExactly(
      LetterFeedback('g', LetterState.CORRECT),
      LetterFeedback('a', LetterState.CORRECT),
      LetterFeedback('l', LetterState.ABSENT),
      LetterFeedback('h', LetterState.ABSENT),
      LetterFeedback('o', LetterState.PRESENT),
    )
  }

  // --- Duplicate letters (spec algorithm) ---

  @Test
  fun `duplicate letter in guess - first correct, second absent`() {
    // Spec example: target = "gatos", guess = "gagas"
    // g[0] → CORRECT, a[1] → CORRECT, g[2] → ABSENT (g used), a[3] → ABSENT (a used), s[4] → CORRECT
    val feedback = GameLogic.calculateLetterFeedback("gagas", "gatos")
    assertThat(feedback).containsExactly(
      LetterFeedback('g', LetterState.CORRECT),
      LetterFeedback('a', LetterState.CORRECT),
      LetterFeedback('g', LetterState.ABSENT),
      LetterFeedback('a', LetterState.ABSENT),
      LetterFeedback('s', LetterState.CORRECT),
    )
  }

  @Test
  fun `duplicate letter in guess - first present, second absent when only one in target`() {
    // target = "abcde", guess = "aaxyz"
    // a[0] → CORRECT, a[1] → ABSENT (only 1 'a' in target, already consumed)
    val feedback = GameLogic.calculateLetterFeedback("aaxyz", "abcde")
    assertThat(feedback[0]).isEqualTo(LetterFeedback('a', LetterState.CORRECT))
    assertThat(feedback[1]).isEqualTo(LetterFeedback('a', LetterState.ABSENT))
  }

  @Test
  fun `duplicate letter in target - both can be marked`() {
    // target = "aabcd", guess = "axayz"
    // a[0] → CORRECT, x → ABSENT, a[2] → PRESENT (second a in target at pos 1), y → ABSENT, z → ABSENT
    val feedback = GameLogic.calculateLetterFeedback("axayz", "aabcd")
    assertThat(feedback[0]).isEqualTo(LetterFeedback('a', LetterState.CORRECT))
    assertThat(feedback[2]).isEqualTo(LetterFeedback('a', LetterState.PRESENT))
  }

  @Test
  fun `triple letter in guess with two in target`() {
    // target = "aabxx", guess = "aaayz"
    // a[0] → CORRECT, a[1] → CORRECT, a[2] → ABSENT (both target a's consumed)
    val feedback = GameLogic.calculateLetterFeedback("aaayz", "aabxx")
    assertThat(feedback[0]).isEqualTo(LetterFeedback('a', LetterState.CORRECT))
    assertThat(feedback[1]).isEqualTo(LetterFeedback('a', LetterState.CORRECT))
    assertThat(feedback[2]).isEqualTo(LetterFeedback('a', LetterState.ABSENT))
  }

  @Test
  fun `left to right processing for duplicates - present before absent`() {
    // target = "baxcd", guess = "aaxyz"
    // Pass 1: a[1] == a[1] → CORRECT (a consumed)
    // Pass 2: a[0] → no 'a' remaining → ABSENT
    // So we need a target where 'a' is NOT at index 1:
    // target = "xyzab", guess = "aaxyz"
    // Pass 1: no exact matches
    // Pass 2: a[0] → 'a' exists (1 remaining) → PRESENT. a[1] → remaining=0 → ABSENT.
    val feedback = GameLogic.calculateLetterFeedback("aaxyz", "xyzab")
    assertThat(feedback[0]).isEqualTo(LetterFeedback('a', LetterState.PRESENT))
    assertThat(feedback[1]).isEqualTo(LetterFeedback('a', LetterState.ABSENT))
  }

  @Test
  fun `correct position takes priority over present for duplicates`() {
    // target = "abcda", guess = "axyza"
    // a[0] → CORRECT, a[4] → CORRECT (both in correct position, both target a's consumed)
    val feedback = GameLogic.calculateLetterFeedback("axyza", "abcda")
    assertThat(feedback[0]).isEqualTo(LetterFeedback('a', LetterState.CORRECT))
    assertThat(feedback[4]).isEqualTo(LetterFeedback('a', LetterState.CORRECT))
  }

  // --- Different word lengths ---

  @Test
  fun `works with 6 letter words`() {
    val feedback = GameLogic.calculateLetterFeedback("abcdef", "abcdef")
    assertThat(feedback).hasSize(6)
    assertThat(feedback.all { it.state == LetterState.CORRECT }).isTrue()
  }

  @Test
  fun `works with 7 letter words`() {
    val feedback = GameLogic.calculateLetterFeedback("abcdefg", "abcdefg")
    assertThat(feedback).hasSize(7)
  }

  @Test
  fun `works with 8 letter words`() {
    val feedback = GameLogic.calculateLetterFeedback("abcdefgh", "abcdefgh")
    assertThat(feedback).hasSize(8)
  }

  @Test
  fun `feedback letters match the guess letters`() {
    val feedback = GameLogic.calculateLetterFeedback("gatos", "mundo")
    assertThat(feedback.map { it.letter }).containsExactly('g', 'a', 't', 'o', 's').inOrder()
  }
}
