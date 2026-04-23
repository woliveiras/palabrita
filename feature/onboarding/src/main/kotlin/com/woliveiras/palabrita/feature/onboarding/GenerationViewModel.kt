package com.woliveiras.palabrita.feature.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.worker.GenerationInfo
import com.woliveiras.palabrita.core.ai.worker.GenerationProgress
import com.woliveiras.palabrita.core.ai.worker.GenerationWorkState
import com.woliveiras.palabrita.core.ai.worker.PuzzleGenerationScheduler
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.data.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StepStatus {
  PENDING,
  IN_PROGRESS,
  COMPLETED,
}

data class GenerationStep(
  @StringRes val labelResId: Int,
  val status: StepStatus,
  val detail: String? = null,
)

data class GenerationState(
  val isGenerating: Boolean = true,
  val isComplete: Boolean = false,
  val failed: Boolean = false,
  val progress: GenerationProgress = GenerationProgress(),
  val steps: List<GenerationStep> = emptyList(),
)

@HiltViewModel
class GenerationViewModel
@Inject
constructor(
  private val generationScheduler: PuzzleGenerationScheduler,
  private val appPreferences: AppPreferences,
  private val modelRepository: ModelRepository,
  private val engineManager: LlmEngineManager,
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
      combine(engineManager.engineState, generationScheduler.observeGenerationInfo()) {
          engineState,
          info ->
          Pair(engineState, info)
        }
        .collect { (engineState, info) ->
          val steps = deriveSteps(engineState, info)
          when (info.state) {
            GenerationWorkState.SUCCEEDED -> {
              val completedSteps = steps.map {
                it.copy(status = StepStatus.COMPLETED, detail = null)
              }
              _state.update {
                it.copy(isGenerating = false, isComplete = true, steps = completedSteps)
              }
            }
            GenerationWorkState.FAILED -> {
              _state.update { it.copy(isGenerating = false, failed = true, steps = steps) }
            }
            GenerationWorkState.RUNNING -> {
              _state.update {
                it.copy(isGenerating = true, progress = info.progress, steps = steps)
              }
            }
            GenerationWorkState.IDLE -> {}
          }
        }
    }
  }

  private fun deriveSteps(engineState: EngineState, info: GenerationInfo): List<GenerationStep> {
    val progress = info.progress
    val hasGenerationProgress = progress.totalExpected > 0

    val verifyStatus =
      when (engineState) {
        is EngineState.Uninitialized -> StepStatus.IN_PROGRESS
        else -> StepStatus.COMPLETED
      }

    val loadStatus =
      when (engineState) {
        is EngineState.Uninitialized -> StepStatus.PENDING
        is EngineState.Initializing -> StepStatus.IN_PROGRESS
        is EngineState.Ready -> StepStatus.COMPLETED
        is EngineState.Error -> StepStatus.IN_PROGRESS
      }

    val initStatus =
      when {
        engineState is EngineState.Ready && hasGenerationProgress -> StepStatus.COMPLETED
        engineState is EngineState.Ready -> StepStatus.IN_PROGRESS
        else -> StepStatus.PENDING
      }

    val generateStatus =
      when {
        !hasGenerationProgress -> StepStatus.PENDING
        info.state == GenerationWorkState.SUCCEEDED -> StepStatus.COMPLETED
        else -> StepStatus.IN_PROGRESS
      }

    val generateDetail =
      if (hasGenerationProgress) "${progress.generatedCount}/${progress.totalExpected}" else null

    return listOf(
      GenerationStep(CommonR.string.generation_step_verify, verifyStatus),
      GenerationStep(CommonR.string.generation_step_load, loadStatus),
      GenerationStep(CommonR.string.generation_step_init, initStatus),
      GenerationStep(CommonR.string.generation_step_generate, generateStatus, generateDetail),
    )
  }

  fun markOnboardingComplete() {
    viewModelScope.launch { appPreferences.setOnboardingComplete() }
  }

  fun cancelGeneration() {
    generationScheduler.cancelGeneration()
  }
}
