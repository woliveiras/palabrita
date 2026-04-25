package com.woliveiras.palabrita.feature.settings

import com.woliveiras.palabrita.core.ai.AiModelInfo
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.ThemeMode

data class SettingsState(
  val stats: PlayerStats = PlayerStats(),
  val currentModel: ModelConfig = ModelConfig(),
  val deviceTier: DeviceTier = DeviceTier.MEDIUM,
  val availableModels: List<AiModelInfo> = emptyList(),
  val isModelPickerVisible: Boolean = false,
  val isThemePickerVisible: Boolean = false,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val isResetDialogVisible: Boolean = false,
  val isResetting: Boolean = false,
) {
  val currentLanguage: String
    get() = stats.preferredLanguage
}
