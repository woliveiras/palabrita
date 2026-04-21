package com.woliveiras.palabrita.core.data.repository

import com.woliveiras.palabrita.core.data.db.dao.PuzzleDao
import com.woliveiras.palabrita.core.data.mapper.toDomain
import com.woliveiras.palabrita.core.data.mapper.toEntity
import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzleRepositoryImpl @Inject constructor(private val puzzleDao: PuzzleDao) :
  PuzzleRepository {

  override suspend fun getNextUnplayed(language: String, difficulty: Int): Puzzle? =
    puzzleDao.getNextUnplayed(language, difficulty)?.toDomain()

  override suspend fun countUnplayed(language: String, difficulty: Int): Int =
    puzzleDao.countUnplayed(language, difficulty)

  override suspend fun countAllUnplayed(language: String): Int =
    puzzleDao.countAllUnplayed(language)

  override suspend fun getAllGeneratedWords(): Set<String> =
    puzzleDao.getAllWords().toSet()

  override suspend fun getRecentWords(limit: Int): List<String> =
    puzzleDao.getRecentWords(limit)

  override suspend fun savePuzzle(puzzle: Puzzle): Long =
    puzzleDao.insert(puzzle.toEntity())

  override suspend fun savePuzzles(puzzles: List<Puzzle>) =
    puzzleDao.insertAll(puzzles.map { it.toEntity() })

  override suspend fun markAsPlayed(puzzleId: Long) {
    puzzleDao.markAsPlayed(puzzleId, System.currentTimeMillis())
  }

  override suspend fun deleteUnplayedAiPuzzles() =
    puzzleDao.deleteUnplayedAiPuzzles()

  override suspend fun markAllUnplayed() =
    puzzleDao.markAllUnplayed()
}
