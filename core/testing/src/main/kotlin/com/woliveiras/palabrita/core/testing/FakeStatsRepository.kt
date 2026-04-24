package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeStatsRepository(initialStats: PlayerStats = PlayerStats()) : StatsRepository {
  private var stats = initialStats
  private val _flow = MutableStateFlow(initialStats)

  override suspend fun getStats(): PlayerStats = stats

  override suspend fun updateAfterGame(won: Boolean, attempts: Int, hintsUsed: Int) {}

  override suspend fun updateLanguage(language: String) {
    stats = stats.copy(preferredLanguage = language)
    _flow.value = stats
  }

  override suspend fun resetProgress() {
    stats = PlayerStats(preferredLanguage = stats.preferredLanguage)
    _flow.value = stats
  }

  override fun observeStats(): Flow<PlayerStats> = _flow
}
