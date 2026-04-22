package com.woliveiras.palabrita.core.data.repository

import com.woliveiras.palabrita.core.data.db.dao.PlayerStatsDao
import com.woliveiras.palabrita.core.data.mapper.toDomain
import com.woliveiras.palabrita.core.data.mapper.toEntity
import com.woliveiras.palabrita.core.model.PlayerStats
import com.woliveiras.palabrita.core.model.PlayerTier
import com.woliveiras.palabrita.core.model.repository.StatsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class StatsRepositoryImpl @Inject constructor(private val statsDao: PlayerStatsDao) :
  StatsRepository {

  override suspend fun getStats(): PlayerStats =
    statsDao.get()?.toDomain() ?: PlayerStats()

  override suspend fun updateAfterGame(
    won: Boolean,
    attempts: Int,
    difficulty: Int,
    hintsUsed: Int,
  ) {
    val current = getStats()
    val now = System.currentTimeMillis()

    val xp = calculateXpForGame(won, attempts, difficulty, hintsUsed)
    val newTotalXp = current.totalXp + xp
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

    val newWonByDiff =
      if (won) {
        current.gamesWonByDifficulty.toMutableMap().apply {
          this[difficulty] = (this[difficulty] ?: 0) + 1
        }
      } else {
        current.gamesWonByDifficulty
      }

    val winsAtDifficulty = newWonByDiff[difficulty] ?: 0
    val gamesAtDifficulty = (current.winRateByDifficulty[difficulty]?.let {
      if (it > 0f) ((current.gamesWonByDifficulty[difficulty] ?: 0) / it).toInt() else 0
    } ?: 0) + 1
    val newWinRate =
      current.winRateByDifficulty.toMutableMap().apply {
        this[difficulty] = if (gamesAtDifficulty > 0) winsAtDifficulty.toFloat() / gamesAtDifficulty else 0f
      }

    val newConsecutiveLosses =
      if (won) 0 else current.consecutiveLossesAtCurrent + 1

    val updated =
      current.copy(
        totalPlayed = newTotalPlayed,
        totalWon = newTotalWon,
        avgAttempts = newAvg,
        totalXp = newTotalXp,
        playerTier = PlayerTier.fromXp(newTotalXp),
        gamesWonByDifficulty = newWonByDiff,
        winRateByDifficulty = newWinRate,
        consecutiveLossesAtCurrent = newConsecutiveLosses,
        guessDistribution = newDistribution,
        lastPlayedAt = now,
      )

    statsDao.upsert(updated.toEntity())
  }

  override suspend fun checkAndPromoteDifficulty(): Int {
    val stats = getStats()
    val current = stats.currentDifficulty
    val winsAtCurrent = stats.gamesWonByDifficulty[current] ?: 0
    val winRateAtCurrent = stats.winRateByDifficulty[current] ?: 0f
    val requiredWins = 5

    val newDifficulty =
      when {
        winsAtCurrent >= requiredWins && winRateAtCurrent >= 0.70f && current < 5 -> current + 1
        stats.consecutiveLossesAtCurrent >= 3 && current > 1 -> current - 1
        else -> current
      }

    if (newDifficulty != current) {
      val updated =
        stats.copy(
          currentDifficulty = newDifficulty,
          maxUnlockedDifficulty = maxOf(stats.maxUnlockedDifficulty, newDifficulty),
          consecutiveLossesAtCurrent = 0,
        )
      statsDao.upsert(updated.toEntity())
    }

    return newDifficulty
  }

  override fun observeStats(): Flow<PlayerStats> =
    statsDao.observe().map { it?.toDomain() ?: PlayerStats() }

  override suspend fun updateLanguage(language: String) {
    val current = getStats()
    statsDao.upsert(current.copy(preferredLanguage = language).toEntity())
  }

  override suspend fun updateWordSizePreference(preference: String) {
    val current = getStats()
    statsDao.upsert(current.copy(wordSizePreference = preference).toEntity())
  }

  override suspend fun resetProgress() {
    val current = getStats()
    val reset = PlayerStats(
      preferredLanguage = current.preferredLanguage,
      wordSizePreference = current.wordSizePreference,
    )
    statsDao.upsert(reset.toEntity())
  }
}

fun calculateXpForGame(
  won: Boolean,
  attempts: Int,
  difficulty: Int,
  hintsUsed: Int,
): Int {
  if (!won) return 0

  val baseXp =
    when (difficulty) {
      1 -> 1
      2 -> 2
      3 -> 3
      4 -> 5
      5 -> 8
      else -> 1
    }

  val attemptBonus =
    when (attempts) {
      1 -> 3
      2 -> 1
      else -> 0
    }

  val hintPenalty = hintsUsed

  return (baseXp + attemptBonus - hintPenalty).coerceAtLeast(1)
}
