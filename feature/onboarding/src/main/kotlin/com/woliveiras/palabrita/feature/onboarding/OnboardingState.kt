package com.woliveiras.palabrita.feature.onboarding

import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.ModelId

data class OnboardingState(
  val currentStep: OnboardingStep = OnboardingStep.WELCOME,
  val selectedLanguage: String = "pt",
  val deviceTier: DeviceTier = DeviceTier.HIGH,
  val selectedModel: ModelId? = null,
  val showTierWarning: Boolean = false,
  val downloadProgress: Float = 0f,
  val downloadedBytes: Long = 0L,
  val totalBytes: Long = 0L,
  val downloadFailed: Boolean = false,
  val downloadErrorMessage: String? = null,
  val generationProgress: GenerationProgress? = null,
  val error: OnboardingError? = null,
)

enum class OnboardingStep {
  WELCOME,
  LANGUAGE,
  MODEL_SELECTION,
  DOWNLOAD,
  GENERATION,
  COMPLETE,
}

data class GenerationProgress(
  val current: Int,
  val total: Int,
  val lastGeneratedWord: String? = null,
)

sealed class OnboardingError {
  data class DownloadFailed(val message: String) : OnboardingError()

  data class GenerationFailed(val validCount: Int, val requiredCount: Int) : OnboardingError()

  data class InsufficientStorage(val requiredBytes: Long, val availableBytes: Long) :
    OnboardingError()
}

sealed class OnboardingAction {
  data object Next : OnboardingAction()

  data object Back : OnboardingAction()

  data class SelectLanguage(val language: String) : OnboardingAction()

  data class SelectModel(val modelId: ModelId) : OnboardingAction()

  data object AutoSelectModel : OnboardingAction()

  data object SkipToLightMode : OnboardingAction()

  data object StartDownload : OnboardingAction()

  data object CancelDownload : OnboardingAction()

  data object RetryDownload : OnboardingAction()

  data object StartGeneration : OnboardingAction()

  data object DismissTierWarning : OnboardingAction()
}
