package com.woliveiras.palabrita.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.DatasetRegistry
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.ModelDownloadManager
import com.woliveiras.palabrita.core.ai.ModelDownloadProgress
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
  deviceTier: DeviceTier,
  datasetRegistry: DatasetRegistry,
  private val statsRepository: StatsRepository,
  private val modelRepository: ModelRepository,
  private val appPreferences: AppPreferences,
  private val downloadManager: ModelDownloadManager,
  private val engineManager: LlmEngineManager,
) : ViewModel() {

  private val languages = datasetRegistry.availableLanguages()
  private val defaultLanguage =
    languages.firstOrNull { it.code == java.util.Locale.getDefault().language }?.code
      ?: languages.firstOrNull()?.code
      ?: "en"

  private val _state =
    MutableStateFlow(
      OnboardingState(
        currentStep = OnboardingStep.WELCOME,
        deviceTier = deviceTier,
        selectedLanguage = defaultLanguage,
        availableLanguages = languages,
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
      is OnboardingAction.DismissTierWarning -> _state.update { it.copy(showTierWarning = false) }
      is OnboardingAction.StartDownload -> startDownload()
      is OnboardingAction.CancelDownload -> cancelDownload()
      is OnboardingAction.RetryDownload -> retryDownload()
      is OnboardingAction.ProceedToGeneration ->
        _state.update { it.copy(currentStep = OnboardingStep.GENERATION) }
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
            _state.update { it.copy(downloadProgress = 1f, downloadFailed = false) }
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
      val nextStep =
        when (current.currentStep) {
          OnboardingStep.WELCOME ->
            if (languages.size <= 1) OnboardingStep.MODEL_SELECTION else OnboardingStep.LANGUAGE
          OnboardingStep.LANGUAGE -> OnboardingStep.MODEL_SELECTION
          OnboardingStep.MODEL_SELECTION -> OnboardingStep.DOWNLOAD
          OnboardingStep.DOWNLOAD -> OnboardingStep.COMPLETE
          OnboardingStep.GENERATION -> OnboardingStep.GENERATION
          OnboardingStep.COMPLETE -> OnboardingStep.COMPLETE
        }
      if (nextStep == OnboardingStep.DOWNLOAD && current.selectedModel != null) {
        startDownloadForModel(current.selectedModel)
      }
      if (nextStep == OnboardingStep.COMPLETE) {
        viewModelScope.launch {
          statsRepository.updateLanguage(current.selectedLanguage)
          appPreferences.setOnboardingComplete()
        }
      }
      current.copy(currentStep = nextStep)
    }
  }

  private fun navigateBack() {
    _state.update { current ->
      val prevStep =
        when (current.currentStep) {
          OnboardingStep.WELCOME -> OnboardingStep.WELCOME
          OnboardingStep.LANGUAGE -> OnboardingStep.WELCOME
          OnboardingStep.MODEL_SELECTION ->
            if (languages.size <= 1) OnboardingStep.WELCOME else OnboardingStep.LANGUAGE
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
    val requiresTierWarning =
      when {
        modelId == ModelId.GEMMA4_E4B -> true
        (modelId == ModelId.GEMMA4_E2B || modelId == ModelId.PHI4_MINI) &&
          tier != DeviceTier.HIGH -> true
        else -> false
      }
    _state.update { it.copy(selectedModel = modelId, showTierWarning = requiresTierWarning) }
  }

  private fun autoSelectModel() {
    val model =
      when (_state.value.deviceTier) {
        DeviceTier.HIGH -> ModelId.GEMMA4_E2B
        DeviceTier.MEDIUM -> ModelId.QWEN3_0_6B
      }
    _state.update { it.copy(selectedModel = model, showTierWarning = false) }
    navigateNext()
  }

  private fun startDownloadForModel(modelId: ModelId) {
    val existingPath = downloadManager.getModelPath(modelId)
    if (existingPath != null) {
      _state.update { it.copy(downloadProgress = 1f) }
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
      try {
        engineManager.initialize(modelPath)

        engineManager.engineState.collect { engineState ->
          when (engineState) {
            is EngineState.Ready -> {
              statsRepository.updateLanguage(_state.value.selectedLanguage)
              _state.update { it.copy(downloadComplete = true) }
              return@collect
            }
            is EngineState.Error -> {
              statsRepository.updateLanguage(_state.value.selectedLanguage)
              _state.update { it.copy(downloadComplete = true) }
              return@collect
            }
            else -> {
              /* waiting */
            }
          }
        }
      } catch (e: Exception) {
        statsRepository.updateLanguage(_state.value.selectedLanguage)
        _state.update { it.copy(downloadComplete = true) }
      }
    }
  }
}
