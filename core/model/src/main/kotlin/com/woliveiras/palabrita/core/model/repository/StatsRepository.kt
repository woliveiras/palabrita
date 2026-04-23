package com.woliveiras.palabrita.core.model.repository

import com.woliveiras.palabrita.core.model.PlayerStats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
  suspend fun getStats(): PlayerStats

  suspend fun updateAfterGame(won: Boolean, attempts: Int, hintsUsed: Int)

  suspend fun updateLanguage(language: String)

  suspend fun resetProgress()

  fun observeStats(): Flow<PlayerStats>
}
