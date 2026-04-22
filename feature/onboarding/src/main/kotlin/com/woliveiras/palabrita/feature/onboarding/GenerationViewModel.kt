package com.woliveiras.palabrita.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.worker.GenerationWorkState
import com.woliveiras.palabrita.core.ai.worker.PuzzleGenerationScheduler
import com.woliveiras.palabrita.core.data.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GenerationState(
  val isGenerating: Boolean = true,
  val isComplete: Boolean = false,
  val failed: Boolean = false,
)

@HiltViewModel
class GenerationViewModel @Inject constructor(
  private val generationScheduler: PuzzleGenerationScheduler,
  private val appPreferences: AppPreferences,
  private val modelRepository: ModelRepository,
) : ViewModel() {

  private val _state = MutableStateFlow(GenerationState())
  val state: StateFlow<GenerationState> = _state.asStateFlow()

  init {
    observeGeneration()
  }

  fun triggerGeneration(modelId: ModelId?) {
    viewModelScope.launch {
      val resolvedModelId = modelId ?: modelRepository.getConfig().modelId
      if (resolvedModelId == ModelId.NONE) {
        _state.update { it.copy(isGenerating = false, failed = true) }
        return@launch
      }
      generationScheduler.scheduleGeneration(resolvedModelId)
    }
  }

  private fun observeGeneration() {
    viewModelScope.launch {
      generationScheduler.observeGenerationState().collect { workState ->
        when (workState) {
          GenerationWorkState.SUCCEEDED -> {
            _state.update { it.copy(isGenerating = false, isComplete = true) }
          }
          GenerationWorkState.FAILED -> {
            _state.update { it.copy(isGenerating = false, failed = true) }
          }
          GenerationWorkState.RUNNING -> {
            _state.update { it.copy(isGenerating = true) }
          }
          GenerationWorkState.IDLE -> {}
        }
      }
    }
  }

  fun markOnboardingComplete() {
    viewModelScope.launch {
      appPreferences.setOnboardingComplete()
    }
  }
}
