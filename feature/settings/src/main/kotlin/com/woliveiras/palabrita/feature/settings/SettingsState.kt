package com.woliveiras.palabrita.feature.settings

import androidx.annotation.StringRes
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.PlayerStats

data class SettingsState(
  val stats: PlayerStats = PlayerStats(),
  val currentModel: ModelConfig = ModelConfig(),
  val deviceTier: DeviceTier = DeviceTier.MEDIUM,
  val storageInfo: StorageInfo = StorageInfo(),
  val isModelSwitching: Boolean = false,
  val downloadProgress: Float? = null,
  @StringRes val errorRes: Int? = null,
  val hasActiveGame: Boolean = false,
) {
  val currentLanguage: String
    get() = stats.preferredLanguage

  val winRate: Int
    get() =
      if (stats.totalPlayed > 0) {
        (stats.totalWon * 100 / stats.totalPlayed)
      } else {
        0
      }
}

data class StorageInfo(
  val modelSizeBytes: Long = 0,
  val databaseSizeBytes: Long = 0,
  val totalSizeBytes: Long = 0,
  val availableSpaceBytes: Long = 0,
)
