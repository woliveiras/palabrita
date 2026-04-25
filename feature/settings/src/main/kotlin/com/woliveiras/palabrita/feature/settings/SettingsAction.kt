package com.woliveiras.palabrita.feature.settings

import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.ThemeMode

sealed class SettingsAction {
  data object ShowModelPicker : SettingsAction()

  data object DismissModelPicker : SettingsAction()

  data class SelectModel(val modelId: ModelId) : SettingsAction()

  data object RegenPuzzles : SettingsAction()

  data object NavigateToLanguageSelection : SettingsAction()

  data object NavigateToAiInfo : SettingsAction()

  data class ChangeTheme(val mode: ThemeMode) : SettingsAction()

  data object ShowThemePicker : SettingsAction()

  data object DismissThemePicker : SettingsAction()
}
