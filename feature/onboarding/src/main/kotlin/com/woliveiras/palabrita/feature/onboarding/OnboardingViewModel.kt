package com.woliveiras.palabrita.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.ModelDownloadManager
import com.woliveiras.palabrita.core.ai.ModelDownloadProgress
import com.woliveiras.palabrita.core.ai.PuzzleGenerator
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.data.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.ModelId
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
class OnboardingViewModel @Inject constructor(
  deviceTier: DeviceTier,
  private val statsRepository: StatsRepository,
  private val modelRepository: ModelRepository,
  private val puzzleRepository: PuzzleRepository,
  private val appPreferences: AppPreferences,
  private val downloadManager: ModelDownloadManager,
  private val engineManager: LlmEngineManager,
  private val puzzleGenerator: PuzzleGenerator,
) : ViewModel() {

  private val _state = MutableStateFlow(
    OnboardingState(
      currentStep = OnboardingStep.WELCOME,
      deviceTier = deviceTier,
      selectedLanguage = java.util.Locale.getDefault().language,
    )
  )
  val state: StateFlow<OnboardingState> = _state.asStateFlow()

  init {
    observeDownloadProgress()
  }

  fun onAction(action: OnboardingAction) {
    when (action) {
      is OnboardingAction.Next -> navigateNext()
      is OnboardingAction.Back -> navigateBack()
      is OnboardingAction.SelectLanguage -> selectLanguage(action.language)
      is OnboardingAction.SelectModel -> selectModel(action.modelId)
      is OnboardingAction.AutoSelectModel -> autoSelectModel()
      is OnboardingAction.SkipToLightMode -> skipToLightMode()
      is OnboardingAction.DismissTierWarning -> _state.update { it.copy(showTierWarning = false) }
      is OnboardingAction.StartDownload -> startDownload()
      is OnboardingAction.CancelDownload -> cancelDownload()
      is OnboardingAction.RetryDownload -> retryDownload()
      is OnboardingAction.StartGeneration -> startGeneration()
    }
  }

  private fun observeDownloadProgress() {
    viewModelScope.launch {
      downloadManager.progress.collect { progress ->
        when (progress) {
          is ModelDownloadProgress.Idle ->
            _state.update {
              it.copy(downloadProgress = 0f, downloadFailed = false, downloadErrorMessage = null)
            }
          is ModelDownloadProgress.Checking ->
            _state.update { it.copy(downloadProgress = 0f, downloadFailed = false) }
          is ModelDownloadProgress.Downloading ->
            _state.update {
              it.copy(
                downloadProgress = progress.progress,
                downloadedBytes = progress.downloadedBytes,
                totalBytes = progress.totalBytes,
                downloadFailed = false,
              )
            }
          is ModelDownloadProgress.Completed -> {
            _state.update {
              it.copy(downloadProgress = 1f, downloadFailed = false)
            }
            initializeEngineAndGenerate(progress.modelPath)
          }
          is ModelDownloadProgress.Failed ->
            _state.update {
              it.copy(downloadFailed = true, downloadErrorMessage = progress.message)
            }
        }
      }
    }
  }

  private fun navigateNext() {
    _state.update { current ->
      val nextStep = when (current.currentStep) {
        OnboardingStep.WELCOME -> OnboardingStep.LANGUAGE
        OnboardingStep.LANGUAGE -> OnboardingStep.MODEL_SELECTION
        OnboardingStep.MODEL_SELECTION -> OnboardingStep.DOWNLOAD
        OnboardingStep.DOWNLOAD -> OnboardingStep.GENERATION
        OnboardingStep.GENERATION -> OnboardingStep.COMPLETE
        OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
      }
      if (nextStep == OnboardingStep.DOWNLOAD && current.selectedModel != null) {
        startDownloadForModel(current.selectedModel)
      }
      if (nextStep == OnboardingStep.COMPLETE) {
        viewModelScope.launch { appPreferences.setOnboardingComplete() }
      }
      current.copy(currentStep = nextStep)
    }
  }

  private fun navigateBack() {
    _state.update { current ->
      val prevStep = when (current.currentStep) {
        OnboardingStep.WELCOME -> OnboardingStep.WELCOME
        OnboardingStep.LANGUAGE -> OnboardingStep.WELCOME
        OnboardingStep.MODEL_SELECTION -> OnboardingStep.LANGUAGE
        OnboardingStep.DOWNLOAD -> {
          downloadManager.cancelDownload()
          OnboardingStep.MODEL_SELECTION
        }
        OnboardingStep.GENERATION -> OnboardingStep.GENERATION
        OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
      }
      current.copy(currentStep = prevStep)
    }
  }

  private fun selectLanguage(language: String) {
    _state.update { it.copy(selectedLanguage = language) }
  }

  private fun selectModel(modelId: ModelId) {
    val tier = _state.value.deviceTier
    val requiresTierWarning = when {
      modelId == ModelId.GEMMA4_E2B && tier != DeviceTier.HIGH -> true
      else -> false
    }
    _state.update {
      it.copy(selectedModel = modelId, showTierWarning = requiresTierWarning)
    }
  }

  private fun autoSelectModel() {
    val model = when (_state.value.deviceTier) {
      DeviceTier.HIGH -> ModelId.GEMMA4_E2B
      DeviceTier.MEDIUM -> ModelId.GEMMA3_1B
      DeviceTier.LOW -> ModelId.NONE
    }
    _state.update { it.copy(selectedModel = model, showTierWarning = false) }
  }

  private fun skipToLightMode() {
    _state.update {
      it.copy(
        selectedModel = ModelId.NONE,
        currentStep = OnboardingStep.GENERATION,
      )
    }
    startGeneration()
  }

  private fun startDownloadForModel(modelId: ModelId) {
    val existingPath = downloadManager.getModelPath(modelId)
    if (existingPath != null) {
      initializeEngineAndGenerate(existingPath)
      return
    }
    viewModelScope.launch { downloadManager.startDownload(modelId) }
  }

  private fun startDownload() {
    val modelId = _state.value.selectedModel ?: return
    viewModelScope.launch { downloadManager.startDownload(modelId) }
  }

  private fun cancelDownload() {
    downloadManager.cancelDownload()
    _state.update { it.copy(currentStep = OnboardingStep.MODEL_SELECTION) }
  }

  private fun retryDownload() {
    val modelId = _state.value.selectedModel ?: return
    _state.update { it.copy(downloadFailed = false, downloadErrorMessage = null) }
    viewModelScope.launch { downloadManager.startDownload(modelId) }
  }

  private fun initializeEngineAndGenerate(modelPath: String) {
    viewModelScope.launch {
      _state.update { it.copy(currentStep = OnboardingStep.GENERATION) }

      try {
        engineManager.initialize(modelPath)

        engineManager.engineState.collect { engineState ->
          when (engineState) {
            is EngineState.Ready -> {
              generatePuzzles()
              return@collect
            }
            is EngineState.Error -> {
              _state.update {
                it.copy(
                  error = OnboardingError.GenerationFailed(0, INITIAL_PUZZLE_COUNT),
                )
              }
              return@collect
            }
            else -> { /* waiting */ }
          }
        }
      } catch (e: Exception) {
        _state.update {
          it.copy(error = OnboardingError.GenerationFailed(0, INITIAL_PUZZLE_COUNT))
        }
      }
    }
  }

  private fun startGeneration() {
    if (_state.value.selectedModel == ModelId.NONE) {
      viewModelScope.launch {
        _state.update {
          it.copy(
            generationProgress = GenerationProgress(1, 1),
            currentStep = OnboardingStep.GENERATION,
          )
        }
        appPreferences.setOnboardingComplete()
        _state.update { it.copy(currentStep = OnboardingStep.COMPLETE) }
      }
      return
    }

    viewModelScope.launch { generatePuzzles() }
  }

  private suspend fun generatePuzzles() {
    val currentState = _state.value
    val modelId = currentState.selectedModel ?: return

    _state.update {
      it.copy(generationProgress = GenerationProgress(0, INITIAL_PUZZLE_COUNT))
    }

    try {
      val puzzles = puzzleGenerator.generateBatch(
        count = INITIAL_PUZZLE_COUNT,
        language = currentState.selectedLanguage,
        targetDifficulty = 1,
        recentWords = emptyList(),
        allExistingWords = emptySet(),
        modelId = modelId,
      )

      puzzles.forEachIndexed { index, puzzle ->
        puzzleRepository.savePuzzle(puzzle)
        _state.update {
          it.copy(generationProgress = GenerationProgress(index + 1, INITIAL_PUZZLE_COUNT))
        }
      }

      val stats = statsRepository.getStats()
      statsRepository.updateLanguage(currentState.selectedLanguage)
      appPreferences.setOnboardingComplete()
      _state.update { it.copy(currentStep = OnboardingStep.COMPLETE) }
    } catch (e: Exception) {
      _state.update {
        it.copy(error = OnboardingError.GenerationFailed(0, INITIAL_PUZZLE_COUNT))
      }
    }
  }

  companion object {
    private const val INITIAL_PUZZLE_COUNT = 7
  }
}
