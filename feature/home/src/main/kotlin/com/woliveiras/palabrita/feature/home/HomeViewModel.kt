package com.woliveiras.palabrita.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.model.PlayerTier
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
  private val statsRepository: StatsRepository,
  private val gameSessionRepository: GameSessionRepository,
  private val puzzleRepository: PuzzleRepository,
) : ViewModel() {

  private val _state = MutableStateFlow(HomeState())
  val state: StateFlow<HomeState> = _state.asStateFlow()

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  fun onAction(action: HomeAction) {
    when (action) {
      is HomeAction.StartDailyChallenge -> { /* navigation handled by UI */ }
      is HomeAction.StartFreePlay -> { /* navigation handled by UI */ }
      is HomeAction.NavigateToChat -> { /* navigation handled by UI */ }
      is HomeAction.DismissGenerationBanner ->
        _state.update { it.copy(generationComplete = false) }
    }
  }

  fun loadHome() {
    viewModelScope.launch {
      _state.update { it.copy(isLoading = true) }

      val stats = statsRepository.getStats()
      val today = LocalDate.now().format(dateFormatter)
      val dailySessions = gameSessionRepository.getDailyChallengesForDate(today)

      val completedCount = dailySessions.count { it.completedAt != null }
      val challenges = buildDailyChallenges(stats.currentDifficulty, dailySessions)

      val winRate = if (stats.totalPlayed > 0) {
        stats.totalWon.toFloat() / stats.totalPlayed
      } else 0f

      val milestone = when {
        stats.currentStreak < 7 -> 7
        stats.currentStreak < 30 -> 30
        stats.currentStreak < 100 -> 100
        else -> 365
      }

      _state.update {
        it.copy(
          streak = stats.currentStreak,
          nextStreakMilestone = milestone,
          dailyChallenges = challenges,
          completedDailies = completedCount,
          allDailiesComplete = completedCount >= 3,
          totalPlayed = stats.totalPlayed,
          winRate = winRate,
          playerTier = PlayerTier.fromXp(stats.totalXp).displayName,
          totalXp = stats.totalXp,
          isLoading = false,
        )
      }
    }
  }

  private fun buildDailyChallenges(
    currentDifficulty: Int,
    dailySessions: List<com.woliveiras.palabrita.core.model.GameSession>,
  ): List<DailyChallenge> {
    val difficulties = listOf(
      (currentDifficulty - 1).coerceAtLeast(1),
      currentDifficulty,
      (currentDifficulty + 1).coerceAtMost(5),
    )

    return (0..2).map { index ->
      val session = dailySessions.find { it.dailyChallengeIndex == index }
      val isCompleted = session?.completedAt != null
      val previousCompleted = when (index) {
        0 -> true
        else -> dailySessions.find { it.dailyChallengeIndex == index - 1 }?.completedAt != null
      }

      DailyChallenge(
        index = index,
        state = when {
          isCompleted -> DailyChallengeState.COMPLETED
          previousCompleted -> DailyChallengeState.AVAILABLE
          else -> DailyChallengeState.LOCKED
        },
        difficulty = difficulties[index],
        categoryHint = session?.let { "???" },
        result = if (isCompleted && session != null) {
          DailyChallengeResult(
            attempts = session.attempts.size,
            won = session.won,
            chatExplored = session.chatExplored,
          )
        } else null,
        puzzleId = session?.puzzleId,
      )
    }
  }
}
