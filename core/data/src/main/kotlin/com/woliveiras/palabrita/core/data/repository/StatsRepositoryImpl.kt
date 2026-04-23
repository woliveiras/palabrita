package com.woliveiras.palabrita.core.data.repository

import com.woliveiras.palabrita.core.data.db.dao.PlayerStatsDao
import com.woliveiras.palabrita.core.data.mapper.toDomain
import com.woliveiras.palabrita.core.data.mapper.toEntity
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StatsRepositoryImpl @Inject constructor(private val statsDao: PlayerStatsDao) :
  StatsRepository {

  override suspend fun getStats(): PlayerStats = statsDao.get()?.toDomain() ?: PlayerStats()

  override suspend fun updateAfterGame(won: Boolean, attempts: Int, hintsUsed: Int) {
    val current = getStats()
    val now = System.currentTimeMillis()

    val newTotalPlayed = current.totalPlayed + 1
    val newTotalWon = if (won) current.totalWon + 1 else current.totalWon

    val newAvg =
      if (won) {
        (current.avgAttempts * current.totalWon + attempts) / newTotalWon
      } else {
        current.avgAttempts
      }

    val newDistribution =
      if (won) {
        current.guessDistribution.toMutableMap().apply {
          this[attempts] = (this[attempts] ?: 0) + 1
        }
      } else {
        current.guessDistribution
      }

    val updated =
      current.copy(
        totalPlayed = newTotalPlayed,
        totalWon = newTotalWon,
        avgAttempts = newAvg,
        guessDistribution = newDistribution,
        lastPlayedAt = now,
      )

    statsDao.upsert(updated.toEntity())
  }

  override fun observeStats(): Flow<PlayerStats> =
    statsDao.observe().map { it?.toDomain() ?: PlayerStats() }

  override suspend fun updateLanguage(language: String) {
    val current = getStats()
    statsDao.upsert(current.copy(preferredLanguage = language).toEntity())
  }

  override suspend fun resetProgress() {
    val current = getStats()
    val reset = PlayerStats(preferredLanguage = current.preferredLanguage)
    statsDao.upsert(reset.toEntity())
  }
}
