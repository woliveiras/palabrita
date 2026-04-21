package com.woliveiras.palabrita.feature.home

data class HomeState(
  val streak: Int = 0,
  val nextStreakMilestone: Int = 7,
  val dailyChallenges: List<DailyChallenge> = emptyList(),
  val completedDailies: Int = 0,
  val allDailiesComplete: Boolean = false,
  val totalPlayed: Int = 0,
  val winRate: Float = 0f,
  val playerTier: String = "Novato",
  val totalXp: Int = 0,
  val isGeneratingPuzzles: Boolean = false,
  val generationComplete: Boolean = false,
  val isLoading: Boolean = true,
)

data class DailyChallenge(
  val index: Int,
  val state: DailyChallengeState,
  val difficulty: Int,
  val categoryHint: String? = null,
  val result: DailyChallengeResult? = null,
  val puzzleId: Long? = null,
)

enum class DailyChallengeState { LOCKED, AVAILABLE, COMPLETED }

data class DailyChallengeResult(
  val attempts: Int,
  val won: Boolean,
  val chatExplored: Boolean,
)
