package com.woliveiras.palabrita.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.woliveiras.palabrita.core.ai.AiModelRegistry
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.ModelRepository
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

private val LANGUAGE_DISPLAY =
  mapOf(
    "pt" to "\uD83C\uDDE7\uD83C\uDDF7 Português",
    "en" to "\uD83C\uDDFA\uD83C\uDDF8 English",
    "es" to "\uD83C\uDDEA\uD83C\uDDF8 Español",
  )

@HiltViewModel
class HomeViewModel
@Inject
constructor(
  private val statsRepository: StatsRepository,
  private val puzzleRepository: PuzzleRepository,
  private val gameSessionRepository: GameSessionRepository,
  private val modelRepository: ModelRepository,
  private val workManager: WorkManager,
) : ViewModel() {

  private val _state = MutableStateFlow(HomeState())
  val state: StateFlow<HomeState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      statsRepository.observeStats().collect { stats ->
        val winRate =
          if (stats.totalPlayed > 0) stats.totalWon.toFloat() / stats.totalPlayed else 0f
        val unplayed = puzzleRepository.countAllUnplayed(stats.preferredLanguage)
        val streak = gameSessionRepository.getCurrentStreak()
        val languageDisplay = buildLanguageDisplay(stats.preferredLanguage)

        _state.update {
          it.copy(
            totalPlayed = stats.totalPlayed,
            winRate = winRate,
            unplayedCount = unplayed,
            currentStreak = streak,
            languageDisplay = languageDisplay,
            isLoading = false,
          )
        }
      }
    }
    observeGeneration()
  }

  fun onAction(action: HomeAction) {
    when (action) {
      is HomeAction.Play,
      is HomeAction.GenerateMore,
      is HomeAction.OpenSettings,
      is HomeAction.OpenAboutAi -> {
        /* navigation handled by UI */
      }
      is HomeAction.OpenHowToPlay -> _state.update { it.copy(showHowToPlay = true) }
      is HomeAction.DismissHowToPlay -> _state.update { it.copy(showHowToPlay = false) }
      is HomeAction.DismissGenerationBanner -> _state.update { it.copy(generationComplete = false) }
    }
  }

  private suspend fun buildLanguageDisplay(language: String): String {
    val langLabel = LANGUAGE_DISPLAY[language] ?: language
    val config = modelRepository.getConfig()
    if (config.modelId == ModelId.NONE) return langLabel
    val modelName = AiModelRegistry.getInfo(config.modelId)?.displayName ?: return langLabel
    return "$langLabel \u2022 $modelName"
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
