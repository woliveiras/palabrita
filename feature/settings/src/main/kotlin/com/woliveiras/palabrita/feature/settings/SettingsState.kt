package com.woliveiras.palabrita.feature.settings

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
  val error: String? = null,
  val hasActiveGame: Boolean = false,
) {
  val currentLanguage: String get() = stats.preferredLanguage
  val wordSizePreference: String get() = stats.wordSizePreference
  val isWordSizeUnlocked: Boolean get() = stats.playerTier >= PlayerTier.ASTUTO
  val isEpicWordSizeAvailable: Boolean get() = stats.playerTier >= PlayerTier.EPICO
  val winRate: Int
    get() = if (stats.totalPlayed > 0) {
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
  val label: String,
  val description: String,
)

val WORD_SIZE_OPTIONS = listOf(
  WordSizeOption("DEFAULT", "Padrão", "Dinâmico por dificuldade (5-8)"),
  WordSizeOption("SHORT", "Palavras curtas", "5-6 letras"),
  WordSizeOption("LONG", "Palavras longas", "7-9 letras"),
  WordSizeOption("EPIC", "Palavras épicas", "8-10 letras"),
)

fun generateShareStatsText(stats: PlayerStats): String {
  val winRate = if (stats.totalPlayed > 0) {
    stats.totalWon * 100 / stats.totalPlayed
  } else {
    0
  }
  return buildString {
    appendLine("Palabrita")
    appendLine("${stats.totalPlayed} jogos · ${winRate}% vitórias")
    append("Sequência: ${stats.currentStreak}")
  }
}
