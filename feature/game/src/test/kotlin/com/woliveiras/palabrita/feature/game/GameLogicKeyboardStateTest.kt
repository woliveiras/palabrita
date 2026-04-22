package com.woliveiras.palabrita.feature.game

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GameLogicKeyboardStateTest {

  @Test
  fun `correct letter updates keyboard to CORRECT`() {
    val feedback =
      listOf(LetterFeedback('a', LetterState.CORRECT), LetterFeedback('b', LetterState.ABSENT))
    val result = GameLogic.updateKeyboardState(emptyMap(), feedback)
    assertThat(result['a']).isEqualTo(LetterState.CORRECT)
  }

  @Test
  fun `absent letter updates keyboard to ABSENT`() {
    val feedback = listOf(LetterFeedback('x', LetterState.ABSENT))
    val result = GameLogic.updateKeyboardState(emptyMap(), feedback)
    assertThat(result['x']).isEqualTo(LetterState.ABSENT)
  }

  @Test
  fun `present letter updates keyboard to PRESENT`() {
    val feedback = listOf(LetterFeedback('m', LetterState.PRESENT))
    val result = GameLogic.updateKeyboardState(emptyMap(), feedback)
    assertThat(result['m']).isEqualTo(LetterState.PRESENT)
  }

  @Test
  fun `CORRECT overrides PRESENT from previous attempt`() {
    val previous = mapOf('a' to LetterState.PRESENT)
    val feedback = listOf(LetterFeedback('a', LetterState.CORRECT))
    val result = GameLogic.updateKeyboardState(previous, feedback)
    assertThat(result['a']).isEqualTo(LetterState.CORRECT)
  }

  @Test
  fun `CORRECT is never overridden by PRESENT`() {
    val previous = mapOf('a' to LetterState.CORRECT)
    val feedback = listOf(LetterFeedback('a', LetterState.PRESENT))
    val result = GameLogic.updateKeyboardState(previous, feedback)
    assertThat(result['a']).isEqualTo(LetterState.CORRECT)
  }

  @Test
  fun `CORRECT is never overridden by ABSENT`() {
    val previous = mapOf('a' to LetterState.CORRECT)
    val feedback = listOf(LetterFeedback('a', LetterState.ABSENT))
    val result = GameLogic.updateKeyboardState(previous, feedback)
    assertThat(result['a']).isEqualTo(LetterState.CORRECT)
  }

  @Test
  fun `PRESENT is never overridden by ABSENT`() {
    val previous = mapOf('a' to LetterState.PRESENT)
    val feedback = listOf(LetterFeedback('a', LetterState.ABSENT))
    val result = GameLogic.updateKeyboardState(previous, feedback)
    assertThat(result['a']).isEqualTo(LetterState.PRESENT)
  }

  @Test
  fun `multiple letters update correctly in one pass`() {
    val feedback =
      listOf(
        LetterFeedback('a', LetterState.CORRECT),
        LetterFeedback('b', LetterState.PRESENT),
        LetterFeedback('c', LetterState.ABSENT),
        LetterFeedback('d', LetterState.CORRECT),
        LetterFeedback('e', LetterState.ABSENT),
      )
    val result = GameLogic.updateKeyboardState(emptyMap(), feedback)
    assertThat(result['a']).isEqualTo(LetterState.CORRECT)
    assertThat(result['b']).isEqualTo(LetterState.PRESENT)
    assertThat(result['c']).isEqualTo(LetterState.ABSENT)
    assertThat(result['d']).isEqualTo(LetterState.CORRECT)
    assertThat(result['e']).isEqualTo(LetterState.ABSENT)
  }

  @Test
  fun `untouched keys remain unchanged`() {
    val previous = mapOf('z' to LetterState.ABSENT)
    val feedback = listOf(LetterFeedback('a', LetterState.CORRECT))
    val result = GameLogic.updateKeyboardState(previous, feedback)
    assertThat(result['z']).isEqualTo(LetterState.ABSENT)
    assertThat(result['a']).isEqualTo(LetterState.CORRECT)
  }
}
