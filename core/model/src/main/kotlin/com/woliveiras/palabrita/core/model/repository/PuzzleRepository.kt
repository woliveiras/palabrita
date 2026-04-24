package com.woliveiras.palabrita.core.model.repository

import com.woliveiras.palabrita.core.model.Puzzle

interface PuzzleRepository {
  suspend fun getNextUnplayed(language: String): Puzzle?

  suspend fun countAllUnplayed(language: String): Int

  suspend fun getAllGeneratedWords(): Set<String>

  suspend fun getRecentWords(limit: Int = 50): List<String>

  suspend fun savePuzzle(puzzle: Puzzle): Long

  suspend fun savePuzzles(puzzles: List<Puzzle>)

  suspend fun markAsPlayed(puzzleId: Long)

  suspend fun deleteUnplayedAiPuzzles()

  suspend fun markAllUnplayed()

  suspend fun deleteAll()

  suspend fun getById(id: Long): Puzzle?
}
