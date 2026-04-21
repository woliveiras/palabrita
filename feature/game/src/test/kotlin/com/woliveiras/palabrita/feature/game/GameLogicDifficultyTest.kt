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

  // --- buildDifficultyOptions ---

  @Test
  fun `new player has only level 1 unlocked`() {
    val options = GameLogic.buildDifficultyOptions(currentDifficulty = 1, maxUnlockedDifficulty = 1)
    assertThat(options.filter { it.isUnlocked }.map { it.level }).containsExactly(1)
  }

  @Test
  fun `can try one level above max unlocked`() {
    val options = GameLogic.buildDifficultyOptions(currentDifficulty = 1, maxUnlockedDifficulty = 1)
    // Level 2 should appear but locked
    assertThat(options.map { it.level }).contains(2)
  }

  @Test
  fun `all 5 difficulty levels are shown`() {
    val options = GameLogic.buildDifficultyOptions(currentDifficulty = 1, maxUnlockedDifficulty = 1)
    assertThat(options).hasSize(5)
    assertThat(options.map { it.level }).containsExactly(1, 2, 3, 4, 5).inOrder()
  }

  @Test
  fun `recommended is the current difficulty`() {
    val options = GameLogic.buildDifficultyOptions(currentDifficulty = 2, maxUnlockedDifficulty = 3)
    val recommended = options.filter { it.isRecommended }
    assertThat(recommended).hasSize(1)
    assertThat(recommended[0].level).isEqualTo(2)
  }

  @Test
  fun `unlocked levels include up to maxUnlockedDifficulty`() {
    val options = GameLogic.buildDifficultyOptions(currentDifficulty = 2, maxUnlockedDifficulty = 3)
    assertThat(options.filter { it.isUnlocked }.map { it.level }).containsExactly(1, 2, 3)
  }

  @Test
  fun `levels above maxUnlocked plus one are locked`() {
    val options = GameLogic.buildDifficultyOptions(currentDifficulty = 1, maxUnlockedDifficulty = 2)
    // Levels 1, 2 unlocked; 3 unlocked (one above); 4, 5 locked
    assertThat(options.first { it.level == 4 }.isUnlocked).isFalse()
    assertThat(options.first { it.level == 5 }.isUnlocked).isFalse()
  }

  @Test
  fun `base XP matches spec`() {
    val options = GameLogic.buildDifficultyOptions(currentDifficulty = 1, maxUnlockedDifficulty = 5)
    assertThat(options.map { it.baseXp }).containsExactly(1, 2, 3, 5, 8).inOrder()
  }

  @Test
  fun `labelRes are set for all levels`() {
    val options = GameLogic.buildDifficultyOptions(currentDifficulty = 1, maxUnlockedDifficulty = 5)
    assertThat(options.map { it.labelRes }).containsExactly(
      com.woliveiras.palabrita.core.common.R.string.difficulty_easy,
      com.woliveiras.palabrita.core.common.R.string.difficulty_normal,
      com.woliveiras.palabrita.core.common.R.string.difficulty_hard,
      com.woliveiras.palabrita.core.common.R.string.difficulty_challenging,
      com.woliveiras.palabrita.core.common.R.string.difficulty_expert,
    ).inOrder()
  }
}
