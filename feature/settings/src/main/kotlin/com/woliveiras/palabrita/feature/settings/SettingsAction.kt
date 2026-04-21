package com.woliveiras.palabrita.feature.settings

import com.woliveiras.palabrita.core.model.ModelId

sealed class SettingsAction {
  data class ChangeLanguage(val language: String) : SettingsAction()
  data class ChangeWordSize(val preference: String) : SettingsAction()
  data class SwitchModel(val newModelId: ModelId) : SettingsAction()
  data object DeleteModel : SettingsAction()
  data object ResetProgress : SettingsAction()
  data object ShareStats : SettingsAction()
  data object DismissError : SettingsAction()
}
