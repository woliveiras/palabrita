package com.woliveiras.palabrita.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LanguageSelectionState(
  val appLanguage: String = "en",
  val gameLanguage: String = "en",
  val pendingGameLanguage: String? = null,
  val showConfirmDialog: Boolean = false,
)

sealed class LanguageSelectionAction {
  data class ChangeAppLanguage(val language: String) : LanguageSelectionAction()

  data class ChangeGameLanguage(val language: String) : LanguageSelectionAction()

  data class ConfirmGameLanguage(val generateNow: Boolean) : LanguageSelectionAction()

  data object DismissDialog : LanguageSelectionAction()
}

sealed class LanguageSelectionEvent {
  data class RestartForLocale(val language: String) : LanguageSelectionEvent()

  data class NavigateToGeneration(val language: String) : LanguageSelectionEvent()

  data object NavigateBack : LanguageSelectionEvent()
}

@HiltViewModel
class LanguageSelectionViewModel
@Inject
constructor(
  private val appPreferences: AppPreferences,
  private val statsRepository: StatsRepository,
  private val puzzleRepository: PuzzleRepository,
) : ViewModel() {

  private val _state = MutableStateFlow(LanguageSelectionState())
  val state: StateFlow<LanguageSelectionState> = _state.asStateFlow()

  private val _events = MutableSharedFlow<LanguageSelectionEvent>()
  val events: SharedFlow<LanguageSelectionEvent> = _events.asSharedFlow()

  init {
    viewModelScope.launch {
      val appLang = appPreferences.appLanguage.first()
      val stats = statsRepository.getStats()
      _state.update { it.copy(appLanguage = appLang, gameLanguage = stats.preferredLanguage) }
    }
  }

  fun onAction(action: LanguageSelectionAction) {
    when (action) {
      is LanguageSelectionAction.ChangeAppLanguage -> changeAppLanguage(action.language)
      is LanguageSelectionAction.ChangeGameLanguage -> changeGameLanguage(action.language)
      is LanguageSelectionAction.ConfirmGameLanguage -> confirmGameLanguage(action.generateNow)
      is LanguageSelectionAction.DismissDialog ->
        _state.update { it.copy(pendingGameLanguage = null, showConfirmDialog = false) }
    }
  }

  private fun changeAppLanguage(language: String) {
    if (language == _state.value.appLanguage) return
    viewModelScope.launch {
      appPreferences.setAppLanguage(language)
      _state.update { it.copy(appLanguage = language) }
      _events.emit(LanguageSelectionEvent.RestartForLocale(language))
    }
  }

  private fun changeGameLanguage(language: String) {
    if (language == _state.value.gameLanguage) {
      _state.update { it.copy(pendingGameLanguage = null, showConfirmDialog = false) }
      return
    }
    _state.update { it.copy(pendingGameLanguage = language, showConfirmDialog = true) }
  }

  private fun confirmGameLanguage(generateNow: Boolean) {
    val pending = _state.value.pendingGameLanguage ?: return
    val oldLanguage = _state.value.gameLanguage
    _state.update {
      it.copy(pendingGameLanguage = null, showConfirmDialog = false, gameLanguage = pending)
    }
    viewModelScope.launch {
      statsRepository.updateLanguage(pending)
      puzzleRepository.deleteUnplayedByLanguage(oldLanguage)
      if (generateNow) {
        _events.emit(LanguageSelectionEvent.NavigateToGeneration(pending))
      } else {
        _events.emit(LanguageSelectionEvent.NavigateBack)
      }
    }
  }
}
