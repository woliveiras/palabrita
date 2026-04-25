package com.woliveiras.palabrita.feature.onboarding

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.GenerationActivity
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.PuzzleGenerator
import com.woliveiras.palabrita.core.ai.worker.GenerationInfo
import com.woliveiras.palabrita.core.ai.worker.GenerationProgress
import com.woliveiras.palabrita.core.ai.worker.GenerationWorkState
import com.woliveiras.palabrita.core.ai.worker.PuzzleGenerationScheduler
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
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
  val cancelled: Boolean = false,
  val progress: GenerationProgress = GenerationProgress(),
  val steps: List<GenerationStep> = emptyList(),
  val currentActivityResId: Int? = null,
)

@HiltViewModel
class GenerationViewModel
@Inject
constructor(
  private val generationScheduler: PuzzleGenerationScheduler,
  private val appPreferences: AppPreferences,
  private val modelRepository: ModelRepository,
  private val engineManager: LlmEngineManager,
  private val puzzleGenerator: PuzzleGenerator,
) : ViewModel() {

  private val _state = MutableStateFlow(GenerationState())
  val state: StateFlow<GenerationState> = _state.asStateFlow()
  private var hasTriggered = false
  private var hasSeenRunning = false
  private var expectedWorkId: UUID? = null

  init {
    observeGeneration()
    observeActivity()
  }

  fun triggerGeneration(modelId: ModelId?) {
    viewModelScope.launch {
      val resolvedModelId = modelId ?: modelRepository.getConfig().modelId
      if (resolvedModelId == ModelId.NONE) {
        _state.update { it.copy(isGenerating = false, failed = true) }
        return@launch
      }
      hasTriggered = true
      hasSeenRunning = false
      expectedWorkId = null
      _state.update { GenerationState() }

      if (!engineManager.isReady()) {
        val config = modelRepository.getConfig()
        val modelPath = config.modelPath
        if (modelPath != null) {
          engineManager.initialize(modelPath)
        }
      }

      expectedWorkId = generationScheduler.scheduleGeneration(resolvedModelId)
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
              if (!hasTriggered || !hasSeenRunning) return@collect
              val expected = expectedWorkId
              if (expected != null && info.workId != null && info.workId != expected) {
                return@collect
              }
              val progress = info.progress
              // totalExpected == -1 → worker skipped (already had enough puzzles). Ignore.
              if (progress.totalExpected == -1) return@collect
              // totalExpected > 0 means the worker actually tried to generate but produced nothing
              val allRetriesFailed = progress.totalExpected > 0 && progress.generatedCount == 0
              if (allRetriesFailed) {
                _state.update {
                  it.copy(
                    isGenerating = false,
                    failed = true,
                    progress = progress,
                    steps = steps,
                    currentActivityResId = null,
                  )
                }
                return@collect
              }
              val completedSteps = steps.map {
                it.copy(status = StepStatus.COMPLETED, detail = null)
              }
              _state.update {
                it.copy(
                  isGenerating = false,
                  isComplete = true,
                  progress = progress,
                  steps = completedSteps,
                  currentActivityResId = null,
                )
              }
            }
            GenerationWorkState.FAILED -> {
              if (!hasTriggered || !hasSeenRunning) return@collect
              val expected = expectedWorkId
              if (expected != null && info.workId != null && info.workId != expected) {
                return@collect
              }
              _state.update {
                it.copy(
                  isGenerating = false,
                  failed = true,
                  steps = steps,
                  currentActivityResId = null,
                )
              }
            }
            GenerationWorkState.RUNNING -> {
              hasSeenRunning = true
              _state.update {
                it.copy(
                  isGenerating = true,
                  isComplete = false,
                  failed = false,
                  progress = info.progress,
                  steps = steps,
                )
              }
            }
            GenerationWorkState.IDLE -> {
              if (hasTriggered) {
                _state.update { it.copy(steps = steps) }
              }
            }
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
      when (engineState) {
          is EngineState.Ready if hasGenerationProgress -> StepStatus.COMPLETED
          is EngineState.Ready -> StepStatus.IN_PROGRESS
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
