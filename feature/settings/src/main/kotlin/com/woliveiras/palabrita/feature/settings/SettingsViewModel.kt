package com.woliveiras.palabrita.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.ModelDownloadManager
import com.woliveiras.palabrita.core.ai.ModelRegistry
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.repository.ChatRepository
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
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
  data class NavigateToModelDownload(val modelId: ModelId) : SettingsEvent()

  data object NavigateToGeneration : SettingsEvent()

  data object NavigateToLanguageSelection : SettingsEvent()

  data object NavigateToAiInfo : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  private val statsRepository: StatsRepository,
  private val modelRepository: ModelRepository,
  private val deviceTier: DeviceTier,
  private val modelRegistry: ModelRegistry,
  private val downloadManager: ModelDownloadManager,
  private val appPreferences: AppPreferences,
  private val gameSessionRepository: GameSessionRepository,
  private val chatRepository: ChatRepository,
  private val puzzleRepository: PuzzleRepository,
) : ViewModel() {

  private val _state = MutableStateFlow(SettingsState())
  val state: StateFlow<SettingsState> = _state.asStateFlow()

  private val _events = MutableSharedFlow<SettingsEvent>()
  val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

  init {
    loadData()
    viewModelScope.launch {
      statsRepository.observeStats().collect { stats -> _state.update { it.copy(stats = stats) } }
    }
    viewModelScope.launch {
      appPreferences.themeMode.collect { mode -> _state.update { it.copy(themeMode = mode) } }
    }
  }

  fun onAction(action: SettingsAction) {
    when (action) {
      is SettingsAction.ShowModelPicker -> _state.update { it.copy(isModelPickerVisible = true) }
      is SettingsAction.DismissModelPicker ->
        _state.update { it.copy(isModelPickerVisible = false) }
      is SettingsAction.SelectModel -> selectModel(action.modelId)
      is SettingsAction.RegenPuzzles ->
        viewModelScope.launch { _events.emit(SettingsEvent.NavigateToGeneration) }
      is SettingsAction.NavigateToLanguageSelection ->
        viewModelScope.launch { _events.emit(SettingsEvent.NavigateToLanguageSelection) }
      is SettingsAction.NavigateToAiInfo ->
        viewModelScope.launch { _events.emit(SettingsEvent.NavigateToAiInfo) }
      is SettingsAction.ChangeTheme -> {
        _state.update { it.copy(isThemePickerVisible = false) }
        viewModelScope.launch { appPreferences.setThemeMode(action.mode) }
      }
      is SettingsAction.ShowThemePicker -> _state.update { it.copy(isThemePickerVisible = true) }
      is SettingsAction.DismissThemePicker ->
        _state.update { it.copy(isThemePickerVisible = false) }
      is SettingsAction.ShowResetDialog -> _state.update { it.copy(isResetDialogVisible = true) }
      is SettingsAction.DismissResetDialog ->
        _state.update { it.copy(isResetDialogVisible = false) }
      is SettingsAction.ConfirmReset -> resetProgress()
    }
  }

  private fun resetProgress() {
    viewModelScope.launch {
      _state.update { it.copy(isResetDialogVisible = false, isResetting = true) }
      // Delete ALL AI puzzles (played + unplayed) and restore static puzzles to unplayed FIRST,
      // so that HomeViewModel reads the correct count when stats emission triggers a refresh.
      puzzleRepository.deleteAllAiPuzzles()
      puzzleRepository.markAllUnplayed()
      gameSessionRepository.deleteAll()
      chatRepository.deleteAll()
      appPreferences.resetGenerationCycle()
      // Reset stats last — this triggers HomeViewModel.observeStats() which re-reads
      // countAllUnplayed.
      statsRepository.resetProgress()
      _state.update { it.copy(isResetting = false) }
    }
  }

  private fun loadData() {
    viewModelScope.launch {
      val config = modelRepository.getConfig()
      _state.update {
        it.copy(
          currentModel = config,
          deviceTier = deviceTier,
          availableModels = modelRegistry.allModels(),
        )
      }
    }
  }

  private fun selectModel(modelId: ModelId) {
    _state.update { it.copy(isModelPickerVisible = false) }
    if (modelId == _state.value.currentModel.modelId) return

    // Light Mode (NONE) has no file to download — activate directly
    if (modelId == ModelId.NONE) {
      viewModelScope.launch {
        modelRepository.updateConfig(
          ModelConfig(
            modelId = ModelId.NONE,
            downloadState = DownloadState.DOWNLOADED,
            selectedAt = System.currentTimeMillis(),
          )
        )
        _state.update { it.copy(currentModel = modelRepository.getConfig()) }
      }
      return
    }

    val modelInfo = _state.value.availableModels.firstOrNull { it.modelId == modelId }
    if (modelInfo == null) return

    viewModelScope.launch {
      val modelPath = downloadManager.getModelPath(modelId)
      if (modelPath != null) {
        // File already on disk — activate without re-downloading
        modelRepository.updateConfig(
          ModelConfig(
            modelId = modelId,
            downloadState = DownloadState.DOWNLOADED,
            modelPath = modelPath,
            sizeBytes = modelInfo.sizeBytes,
            selectedAt = System.currentTimeMillis(),
          )
        )
        _state.update { it.copy(currentModel = modelRepository.getConfig()) }
      } else {
        _events.emit(SettingsEvent.NavigateToModelDownload(modelId))
      }
    }
  }
}
