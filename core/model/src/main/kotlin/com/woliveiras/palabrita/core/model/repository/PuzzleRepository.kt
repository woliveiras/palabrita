package com.woliveiras.palabrita.core.model.repository

import com.woliveiras.palabrita.core.model.Puzzle

interface PuzzleRepository {
  suspend fun getNextUnplayed(language: String, difficulty: Int): Puzzle?

  suspend fun countUnplayed(language: String, difficulty: Int): Int

  suspend fun getAllGeneratedWords(): Set<String>

  suspend fun getRecentWords(limit: Int = 50): List<String>

  suspend fun savePuzzle(puzzle: Puzzle): Long

  suspend fun markAsPlayed(puzzleId: Long)
}
