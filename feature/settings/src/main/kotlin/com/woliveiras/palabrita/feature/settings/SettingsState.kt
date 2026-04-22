package com.woliveiras.palabrita.feature.settings

import androidx.annotation.StringRes
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.ModelConfig
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.PlayerTier

data class SettingsState(
  val stats: PlayerStats = PlayerStats(),
  val currentModel: ModelConfig = ModelConfig(),
  val deviceTier: DeviceTier = DeviceTier.LOW,
  val storageInfo: StorageInfo = StorageInfo(),
  val isModelSwitching: Boolean = false,
  val downloadProgress: Float? = null,
  @StringRes val errorRes: Int? = null,
  val hasActiveGame: Boolean = false,
) {
  val currentLanguage: String
    get() = stats.preferredLanguage

  val wordSizePreference: String
    get() = stats.wordSizePreference

  val isWordSizeUnlocked: Boolean
    get() = stats.playerTier >= PlayerTier.ASTUTO

  val isEpicWordSizeAvailable: Boolean
    get() = stats.playerTier >= PlayerTier.EPICO

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

data class WordSizeOption(
  val key: String,
  @androidx.annotation.StringRes val labelRes: Int,
  @androidx.annotation.StringRes val descriptionRes: Int,
)

val WORD_SIZE_OPTIONS =
  listOf(
    WordSizeOption(
      "DEFAULT",
      com.woliveiras.palabrita.core.common.R.string.word_size_default_label,
      com.woliveiras.palabrita.core.common.R.string.word_size_default_desc,
    ),
    WordSizeOption(
      "SHORT",
      com.woliveiras.palabrita.core.common.R.string.word_size_short_label,
      com.woliveiras.palabrita.core.common.R.string.word_size_short_desc,
    ),
    WordSizeOption(
      "LONG",
      com.woliveiras.palabrita.core.common.R.string.word_size_long_label,
      com.woliveiras.palabrita.core.common.R.string.word_size_long_desc,
    ),
    WordSizeOption(
      "EPIC",
      com.woliveiras.palabrita.core.common.R.string.word_size_epic_label,
      com.woliveiras.palabrita.core.common.R.string.word_size_epic_desc,
    ),
  )
