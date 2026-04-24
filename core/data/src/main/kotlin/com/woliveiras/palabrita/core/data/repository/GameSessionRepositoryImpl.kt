package com.woliveiras.palabrita.core.data.repository

import com.woliveiras.palabrita.core.data.db.dao.GameSessionDao
import com.woliveiras.palabrita.core.data.mapper.toDomain
import com.woliveiras.palabrita.core.data.mapper.toEntity
import com.woliveiras.palabrita.core.model.GameSession
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

@Singleton
class GameSessionRepositoryImpl @Inject constructor(private val dao: GameSessionDao) :
  GameSessionRepository {

  override suspend fun create(session: GameSession): Long = dao.insert(session.toEntity())

  override suspend fun update(session: GameSession) = dao.update(session.toEntity())

  override suspend fun completeSession(
    puzzleId: Long,
    attempts: List<String>,
    completedAt: Long,
    hintsUsed: Int,
    won: Boolean,
  ) =
    dao.completeSession(
      puzzleId = puzzleId,
      attempts = json.encodeToString(attempts),
      completedAt = completedAt,
      hintsUsed = hintsUsed,
      won = won,
    )

  override suspend fun getByPuzzleId(puzzleId: Long): GameSession? =
    dao.getByPuzzleId(puzzleId)?.toDomain()

  override suspend fun markChatExplored(puzzleId: Long) = dao.markChatExplored(puzzleId)

  override suspend fun getActiveSession(): GameSession? = dao.getActiveSession()?.toDomain()

  override suspend fun hasActiveGame(): Boolean = dao.hasActiveGame()

  override suspend fun deleteAll() = dao.deleteAll()
}
