package com.woliveiras.palabrita.feature.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.GeneratePuzzlesUseCase
import com.woliveiras.palabrita.core.ai.GenerationActivity
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.PuzzleGenerator
import com.woliveiras.palabrita.core.common.R as CommonR
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

data class GenerationProgress(val generatedCount: Int = 0, val totalExpected: Int = 0)

data class GenerationState(
  val isGenerating: Boolean = true,
  val isComplete: Boolean = false,
  val failed: Boolean = false,
  val cancelled: Boolean = false,
  val progress: GenerationProgress = GenerationProgress(),
  val steps: List<GenerationStep> = emptyList(),
  val currentActivityResId: Int? = null,
)

@HiltViewModel
class GenerationViewModel
@Inject
constructor(
  private val generatePuzzlesUseCase: GeneratePuzzlesUseCase,
  private val appPreferences: AppPreferences,
  private val modelRepository: ModelRepository,
  private val statsRepository: StatsRepository,
  private val engineManager: LlmEngineManager,
  private val puzzleGenerator: PuzzleGenerator,
) : ViewModel() {

  private val _state = MutableStateFlow(GenerationState())
  val state: StateFlow<GenerationState> = _state.asStateFlow()

  init {
    observeActivity()
  }

  fun triggerGeneration(modelId: ModelId?) {
    viewModelScope.launch {
      val resolvedModelId = modelId ?: modelRepository.getConfig().modelId
      if (resolvedModelId == ModelId.NONE) {
        _state.update { it.copy(isGenerating = false, failed = true) }
        return@launch
      }

      _state.update { GenerationState() }

      if (!engineManager.isReady()) {
        val modelPath = modelRepository.getConfig().modelPath
        if (modelPath != null) {
          updateSteps(engineManager.engineState.value)
          engineManager.initialize(modelPath)
        }
      }

      val engineState = engineManager.engineState.value
      if (engineState is EngineState.Error) {
        _state.update { it.copy(isGenerating = false, failed = true, steps = buildSteps(engineState, GenerationProgress())) }
        return@launch
      }

      val language = statsRepository.getStats().preferredLanguage
      val result =
        generatePuzzlesUseCase.execute(
          language = language,
          modelId = resolvedModelId,
          onProgress = { successCount, batchSize ->
            val progress = GenerationProgress(successCount, batchSize)
            _state.update {
              it.copy(
                progress = progress,
                steps = buildSteps(engineManager.engineState.value, progress),
              )
            }
          },
        )

      when {
        result.batchSize == -1 -> {
          // Already had enough puzzles — treat as success
          val completedSteps =
            buildSteps(engineManager.engineState.value, GenerationProgress()).map {
              it.copy(status = StepStatus.COMPLETED, detail = null)
            }
          _state.update {
            it.copy(
              isGenerating = false,
              isComplete = true,
              steps = completedSteps,
              currentActivityResId = null,
            )
          }
        }
        result.generatedCount == 0 -> {
          _state.update {
            it.copy(isGenerating = false, failed = true, currentActivityResId = null)
          }
        }
        else -> {
          val finalProgress = GenerationProgress(result.generatedCount, result.batchSize)
          val completedSteps =
            buildSteps(engineManager.engineState.value, finalProgress).map {
              it.copy(status = StepStatus.COMPLETED, detail = null)
            }
          _state.update {
            it.copy(
              isGenerating = false,
              isComplete = true,
              progress = finalProgress,
              steps = completedSteps,
              currentActivityResId = null,
            )
          }
        }
      }
    }
  }

  private fun updateSteps(engineState: EngineState) {
    _state.update { it.copy(steps = buildSteps(engineState, it.progress)) }
  }

  private fun buildSteps(
    engineState: EngineState,
    progress: GenerationProgress,
  ): List<GenerationStep> {
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
      when (engineState) {
        is EngineState.Ready if hasGenerationProgress -> StepStatus.COMPLETED
        is EngineState.Ready -> StepStatus.IN_PROGRESS
        else -> StepStatus.PENDING
      }

    val generateStatus =
      when {
        !hasGenerationProgress -> StepStatus.PENDING
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
    // Cancellation is handled by the coroutine scope — the launch in triggerGeneration will be
    // cancelled when the ViewModel is cleared or when the user navigates away.
    _state.update { it.copy(isGenerating = false, cancelled = true) }
  }

  private fun observeActivity() {
    viewModelScope.launch {
      puzzleGenerator.activity.collect { activity ->
        val current = _state.value
        val resId = if (current.isComplete || current.failed) null else activityToResId(activity)
        _state.update { it.copy(currentActivityResId = resId) }
      }
    }
  }

  private fun activityToResId(activity: GenerationActivity?): Int? =
    when (activity) {
      GenerationActivity.CREATING -> CommonR.string.generation_activity_creating
      GenerationActivity.VALIDATING -> CommonR.string.generation_activity_validating
      GenerationActivity.VALIDATION_FAILED -> CommonR.string.generation_activity_validation_failed
      GenerationActivity.FAILED_RETRYING -> CommonR.string.generation_activity_retrying
      GenerationActivity.ACCEPTED -> CommonR.string.generation_activity_accepted
      null -> null
    }
}
