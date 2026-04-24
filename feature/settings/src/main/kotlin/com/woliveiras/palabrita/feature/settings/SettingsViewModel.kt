package com.woliveiras.palabrita.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.GameRules
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.repository.ChatRepository
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import com.woliveiras.palabrita.core.model.usecase.ResetProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SettingsEvent {
  data class ShareText(val text: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  private val statsRepository: StatsRepository,
  private val modelRepository: ModelRepository,
  private val gameSessionRepository: GameSessionRepository,
  chatRepository: ChatRepository,
  private val puzzleRepository: PuzzleRepository,
  private val deviceTier: DeviceTier,
  appPreferences: com.woliveiras.palabrita.core.model.preferences.AppPreferences,
) : ViewModel() {

  private val resetProgressUseCase =
    ResetProgressUseCase(
      statsRepository,
      gameSessionRepository,
      chatRepository,
      puzzleRepository,
      appPreferences,
    )

  private val _state = MutableStateFlow(SettingsState())
  val state: StateFlow<SettingsState> = _state.asStateFlow()

  private val _events = MutableSharedFlow<SettingsEvent>()
  val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

  init {
    loadData()
    viewModelScope.launch {
      statsRepository.observeStats().collect { stats -> _state.update { it.copy(stats = stats) } }
    }
  }

  fun onAction(action: SettingsAction) {
    when (action) {
      is SettingsAction.ChangeLanguage -> changeLanguage(action.language)
      is SettingsAction.SwitchModel -> switchModel(action.newModelId)
      is SettingsAction.DeleteModel -> deleteModel()
      is SettingsAction.ResetProgress -> resetProgress()
      is SettingsAction.ShareStats -> shareStats()
      is SettingsAction.DismissError -> _state.update { it.copy(errorRes = null) }
    }
  }

  private fun loadData() {
    viewModelScope.launch {
      val stats = statsRepository.getStats()
      val config = modelRepository.getConfig()
      val hasActive = gameSessionRepository.hasActiveGame()
      _state.update {
        it.copy(
          stats = stats,
          currentModel = config,
          deviceTier = deviceTier,
          hasActiveGame = hasActive,
        )
      }
    }
  }

  private fun changeLanguage(language: String) {
    viewModelScope.launch {
      statsRepository.updateLanguage(language)
      _state.update { it.copy(stats = it.stats.copy(preferredLanguage = language)) }
    }
  }

  private fun switchModel(newModelId: ModelId) {
    if (_state.value.hasActiveGame) {
      _state.update {
        it.copy(errorRes = com.woliveiras.palabrita.core.common.R.string.settings_error_active_game)
      }
      return
    }
    viewModelScope.launch {
      _state.update { it.copy(isModelSwitching = true) }
      if (newModelId == ModelId.NONE) {
        val config =
          ModelConfig(
            modelId = ModelId.NONE,
            downloadState = DownloadState.NOT_DOWNLOADED,
            selectedAt = System.currentTimeMillis(),
          )
        modelRepository.updateConfig(config)
        puzzleRepository.deleteUnplayedAiPuzzles()
        _state.update { it.copy(currentModel = config, isModelSwitching = false) }
      } else {
        // V2: actual download flow
        _state.update {
          it.copy(
            isModelSwitching = false,
            errorRes = com.woliveiras.palabrita.core.common.R.string.settings_error_download_soon,
          )
        }
      }
    }
  }

  private fun deleteModel() {
    viewModelScope.launch {
      val config =
        ModelConfig(
          modelId = ModelId.NONE,
          downloadState = DownloadState.NOT_DOWNLOADED,
          selectedAt = System.currentTimeMillis(),
        )
      modelRepository.updateConfig(config)
      puzzleRepository.deleteUnplayedAiPuzzles()
      _state.update { it.copy(currentModel = config) }
    }
  }

  private fun resetProgress() {
    viewModelScope.launch {
      resetProgressUseCase()
      val newStats = statsRepository.getStats()
      _state.update { it.copy(stats = newStats) }
    }
  }

  private fun shareStats() {
    viewModelScope.launch {
      val stats = _state.value.stats
      _events.emit(SettingsEvent.ShareText(formatStatsShareText(stats)))
    }
  }

  companion object {
    internal fun formatStatsShareText(stats: PlayerStats): String {
      val winRate =
        if (stats.totalPlayed > 0) (stats.totalWon * PERCENT_MULTIPLIER / stats.totalPlayed) else 0
      val histogram =
        (1..GameRules.MAX_ATTEMPTS).joinToString("\n") { i ->
          val count = stats.guessDistribution[i] ?: 0
          "$i: ${"█".repeat(count)} $count"
        }
      return buildString {
        appendLine("📊 Palabrita Stats")
        appendLine("🎮 ${ stats.totalPlayed } played · 🏆 ${ stats.totalWon } won ($winRate%)")
        if (stats.guessDistribution.isNotEmpty()) {
          appendLine()
          append(histogram)
        }
      }
    }

    private const val PERCENT_MULTIPLIER = 100
  }
}
