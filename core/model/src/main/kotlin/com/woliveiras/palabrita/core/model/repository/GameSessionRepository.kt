package com.woliveiras.palabrita.core.model.repository

import com.woliveiras.palabrita.core.model.GameSession

interface GameSessionRepository {
  suspend fun create(session: GameSession): Long

  suspend fun update(session: GameSession)

  suspend fun completeSession(
    puzzleId: Long,
    attempts: List<String>,
    completedAt: Long,
    hintsUsed: Int,
    won: Boolean,
  )

  suspend fun getByPuzzleId(puzzleId: Long): GameSession?

  suspend fun getActiveSession(): GameSession?

  suspend fun hasActiveGame(): Boolean

  suspend fun deleteAll()
}
