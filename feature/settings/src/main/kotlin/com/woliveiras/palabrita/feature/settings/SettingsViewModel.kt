package com.woliveiras.palabrita.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.AiModelRegistry
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.repository.ModelRepository
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
    }
  }

  private fun loadData() {
    viewModelScope.launch {
      val stats = statsRepository.getStats()
      val config = modelRepository.getConfig()
      _state.update {
        it.copy(
          stats = stats,
          currentModel = config,
          deviceTier = deviceTier,
          availableModels = AiModelRegistry.allModels(),
        )
      }
    }
  }

  private fun selectModel(modelId: ModelId) {
    _state.update { it.copy(isModelPickerVisible = false) }
    val currentConfig = _state.value.currentModel
    if (modelId == currentConfig.modelId) return

    val config = _state.value.availableModels.firstOrNull { it.modelId == modelId }
    if (config == null) return

    viewModelScope.launch {
      val storedConfig = modelRepository.getConfig()
      val alreadyDownloaded =
        storedConfig.modelId == modelId && storedConfig.downloadState == DownloadState.DOWNLOADED

      if (alreadyDownloaded) {
        // Already downloaded — just update config
        modelRepository.updateConfig(
          ModelConfig(
            modelId = modelId,
            downloadState = DownloadState.DOWNLOADED,
            modelPath = storedConfig.modelPath,
            sizeBytes = storedConfig.sizeBytes,
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
