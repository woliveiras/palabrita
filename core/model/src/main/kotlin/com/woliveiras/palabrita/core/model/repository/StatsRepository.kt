package com.woliveiras.palabrita.core.model.repository

import com.woliveiras.palabrita.core.model.PlayerStats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    suspend fun getStats(): PlayerStats
    suspend fun updateAfterGame(won: Boolean, attempts: Int, difficulty: Int, hintsUsed: Int)
    suspend fun checkAndPromoteDifficulty(): Int
    fun observeStats(): Flow<PlayerStats>
}
