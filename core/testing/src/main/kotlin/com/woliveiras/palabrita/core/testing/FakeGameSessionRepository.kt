package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.model.GameSession
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository

class FakeGameSessionRepository : GameSessionRepository {
  val sessions = mutableListOf<GameSession>()

  override suspend fun create(session: GameSession): Long {
    sessions.add(session)
    return session.id
  }

  override suspend fun update(session: GameSession) {
    sessions.removeAll { it.puzzleId == session.puzzleId }
    sessions.add(session)
  }

  override suspend fun getByPuzzleId(puzzleId: Long): GameSession? = sessions.find {
    it.puzzleId == puzzleId
  }

  override suspend fun hasActiveGame(): Boolean = sessions.any { it.completedAt == null }

  override suspend fun completeSession(
    puzzleId: Long,
    attempts: List<String>,
    completedAt: Long,
    hintsUsed: Int,
    won: Boolean,
  ) {
    val session = sessions.find { it.puzzleId == puzzleId } ?: return
    sessions.remove(session)
    sessions.add(session.copy(completedAt = completedAt, won = won))
  }

  override suspend fun deleteAll() {
    sessions.clear()
  }
}
