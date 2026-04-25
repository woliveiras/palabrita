package com.woliveiras.palabrita.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.ai.AiModelInfo
import com.woliveiras.palabrita.core.ai.ModelRegistry
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
  val modelPath: String? = null,
  val puzzleSystemPrompt: String = "",
  val puzzleSamplePrompt: String = "",
  val chatSamplePrompt: String = "",
)

@HiltViewModel
class AiInfoViewModel
@Inject
constructor(
  private val modelRepository: ModelRepository,
  private val modelRegistry: ModelRegistry,
) : ViewModel() {

  private val _state = MutableStateFlow(AiInfoState())
  val state: StateFlow<AiInfoState> = _state.asStateFlow()

  init {
    loadModelInfo()
  }

  private fun loadModelInfo() {
    viewModelScope.launch {
      modelRepository.observeConfig().collect { config ->
        val info = modelRegistry.getInfo(config.modelId)
        _state.update {
          it.copy(
            modelInfo = info,
            modelId = config.modelId,
            modelPath = config.modelPath,
            puzzleSystemPrompt = PromptTemplates.puzzleSystemPrompt(),
            puzzleSamplePrompt =
              PromptTemplates.puzzleUserPromptLarge(
                language = "pt",
                wordLength = 5,
                recentWords = listOf("gatos", "mesa"),
              ),
            chatSamplePrompt = PromptTemplates.chatSystemPrompt(word = "gatos", language = "pt"),
          )
        }
      }
    }
  }
}
