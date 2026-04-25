package com.woliveiras.palabrita.feature.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.AiModelInfo
import com.woliveiras.palabrita.core.ai.ModelDownloadManager
import com.woliveiras.palabrita.core.ai.ModelDownloadProgress
import com.woliveiras.palabrita.core.ai.ModelRegistry
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.ModelId
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

data class ModelDownloadUiState(
  val modelId: ModelId = ModelId.NONE,
  val modelInfo: AiModelInfo? = null,
  val downloadProgress: Float = 0f,
  val downloadedBytes: Long = 0L,
  val totalBytes: Long = 0L,
  val isDownloading: Boolean = false,
  val isComplete: Boolean = false,
  val errorMessage: String? = null,
)

sealed class ModelDownloadUiAction {
  data object StartDownload : ModelDownloadUiAction()

  data object CancelDownload : ModelDownloadUiAction()

  data object DismissError : ModelDownloadUiAction()
}

sealed class ModelDownloadUiEvent {
  data class NavigateToGeneration(val modelId: ModelId) : ModelDownloadUiEvent()

  data object NavigateBack : ModelDownloadUiEvent()
}

@HiltViewModel
class ModelDownloadViewModel
@Inject
constructor(
  savedStateHandle: SavedStateHandle,
  private val downloadManager: ModelDownloadManager,
  private val modelRepository: ModelRepository,
  private val puzzleRepository: PuzzleRepository,
  private val statsRepository: StatsRepository,
  private val modelRegistry: ModelRegistry,
) : ViewModel() {

  private val modelIdArg: ModelId =
    savedStateHandle.get<String>("modelId")?.let { runCatching { ModelId.valueOf(it) }.getOrNull() }
      ?: ModelId.NONE

  private val _state = MutableStateFlow(ModelDownloadUiState())
  val state: StateFlow<ModelDownloadUiState> = _state.asStateFlow()

  private val _events = MutableSharedFlow<ModelDownloadUiEvent>()
  val events: SharedFlow<ModelDownloadUiEvent> = _events.asSharedFlow()

  init {
    val info = modelRegistry.getInfo(modelIdArg)
    _state.update { it.copy(modelId = modelIdArg, modelInfo = info) }
    observeDownload()
  }

  fun onAction(action: ModelDownloadUiAction) {
    when (action) {
      is ModelDownloadUiAction.StartDownload -> startDownload()
      is ModelDownloadUiAction.CancelDownload -> cancelDownload()
      is ModelDownloadUiAction.DismissError -> _state.update { it.copy(errorMessage = null) }
    }
  }

  private fun startDownload() {
    viewModelScope.launch { downloadManager.startDownload(modelIdArg) }
  }

  private fun cancelDownload() {
    downloadManager.cancelDownload()
    viewModelScope.launch { _events.emit(ModelDownloadUiEvent.NavigateBack) }
  }

  private fun observeDownload() {
    viewModelScope.launch {
      downloadManager.progress.collect { progress ->
        when (progress) {
          is ModelDownloadProgress.Idle ->
            _state.update { it.copy(isDownloading = false, downloadProgress = 0f) }
          is ModelDownloadProgress.Checking ->
            _state.update { it.copy(isDownloading = true, downloadProgress = 0f) }
          is ModelDownloadProgress.Downloading ->
            _state.update {
              it.copy(
                isDownloading = true,
                downloadProgress = progress.progress,
                downloadedBytes = progress.downloadedBytes,
                totalBytes = progress.totalBytes,
              )
            }
          is ModelDownloadProgress.Completed -> onDownloadCompleted(progress.modelPath)
          is ModelDownloadProgress.Failed ->
            _state.update { it.copy(isDownloading = false, errorMessage = progress.message) }
        }
      }
    }
  }

  private suspend fun onDownloadCompleted(modelPath: String) {
    val info = _state.value.modelInfo
    modelRepository.updateConfig(
      ModelConfig(
        modelId = modelIdArg,
        downloadState = DownloadState.DOWNLOADED,
        modelPath = modelPath,
        sizeBytes = info?.sizeBytes ?: 0L,
        selectedAt = System.currentTimeMillis(),
      )
    )
    _state.update { it.copy(isDownloading = false, isComplete = true, downloadProgress = 1f) }

    val language = statsRepository.getStats().preferredLanguage
    val unplayedCount = puzzleRepository.countAllUnplayed(language)
    if (unplayedCount == 0) {
      _events.emit(ModelDownloadUiEvent.NavigateToGeneration(modelIdArg))
    } else {
      _events.emit(ModelDownloadUiEvent.NavigateBack)
    }
  }
}
