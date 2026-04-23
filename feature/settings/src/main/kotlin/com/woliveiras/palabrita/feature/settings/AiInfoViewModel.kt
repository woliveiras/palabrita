package com.woliveiras.palabrita.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.AiModelInfo
import com.woliveiras.palabrita.core.ai.AiModelRegistry
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.ai.LlmEngineManager
import com.woliveiras.palabrita.core.ai.PromptTemplates
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiInfoState(
  val modelInfo: AiModelInfo? = null,
  val modelId: ModelId = ModelId.NONE,
  val engineState: EngineState = EngineState.Uninitialized,
  val modelPath: String? = null,
  val puzzleSystemPrompt: String = "",
  val puzzleSamplePrompt: String = "",
  val chatSamplePrompt: String = "",
  val isLoading: Boolean = true,
)

@HiltViewModel
class AiInfoViewModel
@Inject
constructor(
  private val modelRepository: ModelRepository,
  private val engineManager: LlmEngineManager,
) : ViewModel() {

  private val _state = MutableStateFlow(AiInfoState())
  val state: StateFlow<AiInfoState> = _state.asStateFlow()

  init {
    loadModelInfo()
    observeEngineState()
  }

  private fun loadModelInfo() {
    viewModelScope.launch {
      modelRepository.observeConfig().collect { config ->
        val info = AiModelRegistry.getInfo(config.modelId)
        _state.update {
          it.copy(
            modelInfo = info,
            modelId = config.modelId,
            modelPath = config.modelPath,
            puzzleSystemPrompt = PromptTemplates.puzzleSystemPrompt(),
            puzzleSamplePrompt =
              PromptTemplates.puzzleUserPromptLarge(
                language = "pt",
                difficulty = 3,
                minLength = 4,
                maxLength = 7,
                recentWords = listOf("gatos", "mesa"),
              ),
            chatSamplePrompt =
              PromptTemplates.chatSystemPrompt(
                word = "gatos",
                category = "animal",
                language = "pt",
              ),
            isLoading = false,
          )
        }
      }
    }
  }

  private fun observeEngineState() {
    viewModelScope.launch {
      engineManager.engineState.collect { engine ->
        _state.update { it.copy(engineState = engine) }
      }
    }
  }
}
