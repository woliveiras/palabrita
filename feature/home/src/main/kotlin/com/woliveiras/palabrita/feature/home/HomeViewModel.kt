package com.woliveiras.palabrita.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.woliveiras.palabrita.core.model.PlayerTier
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PUZZLE_GENERATION_WORK_NAME = "puzzle_generation"

@HiltViewModel
class HomeViewModel
@Inject
constructor(
  private val statsRepository: StatsRepository,
  private val puzzleRepository: PuzzleRepository,
  private val workManager: WorkManager,
) : ViewModel() {

  private val _state = MutableStateFlow(HomeState())
  val state: StateFlow<HomeState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      statsRepository.observeStats().collect { stats ->
        val winRate =
          if (stats.totalPlayed > 0) {
            stats.totalWon.toFloat() / stats.totalPlayed
          } else 0f
        val unplayed = puzzleRepository.countAllUnplayed(stats.preferredLanguage)
        _state.update {
          it.copy(
            totalPlayed = stats.totalPlayed,
            winRate = winRate,
            playerTier = PlayerTier.fromXp(stats.totalXp).displayName,
            totalXp = stats.totalXp,
            unplayedCount = unplayed,
            isLoading = false,
          )
        }
      }
    }
    observeGeneration()
  }

  fun onAction(action: HomeAction) {
    when (action) {
      is HomeAction.StartGame -> {
        /* navigation handled by UI */
      }
      is HomeAction.DismissGenerationBanner -> _state.update { it.copy(generationComplete = false) }
    }
  }

  private fun observeGeneration() {
    viewModelScope.launch {
      workManager.getWorkInfosForUniqueWorkLiveData(PUZZLE_GENERATION_WORK_NAME).asFlow().collect {
        workInfos ->
        val info = workInfos.firstOrNull()
        val isRunning =
          info?.state == WorkInfo.State.RUNNING || info?.state == WorkInfo.State.ENQUEUED
        val isComplete = info?.state == WorkInfo.State.SUCCEEDED
        _state.update {
          it.copy(
            isGeneratingPuzzles = isRunning,
            generationComplete = isComplete && !it.isGeneratingPuzzles,
          )
        }
      }
    }
  }
}
