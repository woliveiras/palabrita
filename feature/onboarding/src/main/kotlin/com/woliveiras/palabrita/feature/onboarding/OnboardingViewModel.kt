package com.woliveiras.palabrita.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.data.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.ModelId
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
class OnboardingViewModel @Inject constructor(
  deviceTier: DeviceTier,
  private val statsRepository: StatsRepository,
  private val modelRepository: ModelRepository,
  private val appPreferences: AppPreferences,
) : ViewModel() {

  private val _state = MutableStateFlow(
    OnboardingState(
      currentStep = OnboardingStep.WELCOME,
      deviceTier = deviceTier,
      selectedLanguage = java.util.Locale.getDefault().language,
    )
  )
  val state: StateFlow<OnboardingState> = _state.asStateFlow()

  fun onAction(action: OnboardingAction) {
    when (action) {
      is OnboardingAction.Next -> navigateNext()
      is OnboardingAction.Back -> navigateBack()
      is OnboardingAction.SelectLanguage -> selectLanguage(action.language)
      is OnboardingAction.SelectModel -> selectModel(action.modelId)
      is OnboardingAction.AutoSelectModel -> autoSelectModel()
      is OnboardingAction.SkipToLightMode -> skipToLightMode()
      is OnboardingAction.DismissTierWarning -> _state.update { it.copy(showTierWarning = false) }
      is OnboardingAction.StartDownload,
      is OnboardingAction.CancelDownload,
      is OnboardingAction.RetryDownload,
      is OnboardingAction.StartGeneration -> {
        // Will be implemented when download/generation infrastructure is ready
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
        OnboardingStep.DOWNLOAD -> OnboardingStep.MODEL_SELECTION
        OnboardingStep.GENERATION -> OnboardingStep.GENERATION // no back from generation
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
  }
}
