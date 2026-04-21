package com.woliveiras.palabrita.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.repository.ChatRepository
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
  private val statsRepository: StatsRepository,
  private val modelRepository: ModelRepository,
  private val gameSessionRepository: GameSessionRepository,
  private val chatRepository: ChatRepository,
  private val puzzleRepository: PuzzleRepository,
  private val deviceTier: DeviceTier,
) : ViewModel() {

  private val _state = MutableStateFlow(SettingsState())
  val state: StateFlow<SettingsState> = _state.asStateFlow()

  init {
    loadData()
  }

  fun onAction(action: SettingsAction) {
    when (action) {
      is SettingsAction.ChangeLanguage -> changeLanguage(action.language)
      is SettingsAction.ChangeWordSize -> changeWordSize(action.preference)
      is SettingsAction.SwitchModel -> switchModel(action.newModelId)
      is SettingsAction.DeleteModel -> deleteModel()
      is SettingsAction.ResetProgress -> resetProgress()
      is SettingsAction.ShareStats -> { /* handled by UI via generateShareStatsText */ }
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

  private fun changeWordSize(preference: String) {
    if (!_state.value.isWordSizeUnlocked) return
    viewModelScope.launch {
      statsRepository.updateWordSizePreference(preference)
      _state.update { it.copy(stats = it.stats.copy(wordSizePreference = preference)) }
    }
  }

  private fun switchModel(newModelId: ModelId) {
    if (_state.value.hasActiveGame) {
      _state.update { it.copy(errorRes = com.woliveiras.palabrita.core.common.R.string.settings_error_active_game) }
      return
    }
    viewModelScope.launch {
      _state.update { it.copy(isModelSwitching = true) }
      if (newModelId == ModelId.NONE) {
        val config = ModelConfig(
          modelId = ModelId.NONE,
          downloadState = DownloadState.NOT_DOWNLOADED,
          selectedAt = System.currentTimeMillis(),
        )
        modelRepository.updateConfig(config)
        puzzleRepository.deleteUnplayedAiPuzzles()
        _state.update { it.copy(currentModel = config, isModelSwitching = false) }
      } else {
        // V2: actual download flow
        _state.update { it.copy(isModelSwitching = false, errorRes = com.woliveiras.palabrita.core.common.R.string.settings_error_download_soon) }
      }
    }
  }

  private fun deleteModel() {
    viewModelScope.launch {
      val config = ModelConfig(
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
      val currentLang = _state.value.stats.preferredLanguage
      val currentWordSize = _state.value.stats.wordSizePreference
      statsRepository.resetProgress()
      gameSessionRepository.deleteAll()
      chatRepository.deleteAll()
      puzzleRepository.markAllUnplayed()
      val newStats = statsRepository.getStats()
      _state.update { it.copy(stats = newStats) }
    }
  }
}
