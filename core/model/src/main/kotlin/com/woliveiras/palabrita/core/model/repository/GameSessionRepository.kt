package com.woliveiras.palabrita.core.model.repository

import com.woliveiras.palabrita.core.model.GameSession

interface GameSessionRepository {
  suspend fun create(session: GameSession): Long
  suspend fun update(session: GameSession)
  suspend fun getByPuzzleId(puzzleId: Long): GameSession?
}
