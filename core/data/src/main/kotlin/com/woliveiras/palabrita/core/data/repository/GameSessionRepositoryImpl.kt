package com.woliveiras.palabrita.core.data.repository

import com.woliveiras.palabrita.core.data.db.dao.GameSessionDao
import com.woliveiras.palabrita.core.data.mapper.toDomain
import com.woliveiras.palabrita.core.data.mapper.toEntity
import com.woliveiras.palabrita.core.model.GameSession
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameSessionRepositoryImpl @Inject constructor(
  private val dao: GameSessionDao,
) : GameSessionRepository {

  override suspend fun create(session: GameSession): Long =
    dao.insert(session.toEntity())

  override suspend fun update(session: GameSession) =
    dao.update(session.toEntity())

  override suspend fun getByPuzzleId(puzzleId: Long): GameSession? =
    dao.getByPuzzleId(puzzleId)?.toDomain()

  override suspend fun hasActiveGame(): Boolean =
    dao.hasActiveGame()

  override suspend fun deleteAll() =
    dao.deleteAll()
}
