package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.model.GameSession
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository

class FakeGameSessionRepository : GameSessionRepository {
  val sessions = mutableListOf<GameSession>()

  /** Pre-configure wins per difficulty for mastery gate tests. Key = wordLength (difficulty). */
  val winsPerDifficulty = mutableMapOf<Int, Int>()

  override suspend fun create(session: GameSession): Long {
    sessions.add(session)
    return session.id
  }

  override suspend fun update(session: GameSession) {
    sessions.removeAll { it.puzzleId == session.puzzleId }
    sessions.add(session)
  }

  override suspend fun getByPuzzleId(puzzleId: Long): GameSession? =
    sessions.find { it.puzzleId == puzzleId }

  override suspend fun getActiveSession(): GameSession? = sessions.find { it.completedAt == null }

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

  override suspend fun getCurrentStreak(): Int =
    sessions
      .filter { it.completedAt != null }
      .sortedByDescending { it.completedAt }
      .takeWhile { it.won }
      .count()

  override suspend fun countWinsByDifficulty(difficulty: Int, language: String): Int =
    winsPerDifficulty[difficulty] ?: 0

  override suspend fun deleteAll() {
    sessions.clear()
  }
}
