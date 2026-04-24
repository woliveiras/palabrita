package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.model.Puzzle
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository

class FakePuzzleRepository(private val puzzle: Puzzle? = null) : PuzzleRepository {
  val markedPlayed = mutableListOf<Long>()
  var unplayedAiPuzzlesCleared = false
  var allUnplayed = false
  var allDeleted = false
  val savedPuzzles = mutableListOf<Puzzle>()

  override suspend fun getNextUnplayed(language: String): Puzzle? = puzzle

  override suspend fun countAllUnplayed(language: String): Int = if (puzzle != null) 1 else 0

  override suspend fun getAllGeneratedWords(): Set<String> = emptySet()

  override suspend fun getRecentWords(limit: Int): List<String> = emptyList()

  override suspend fun savePuzzle(puzzle: Puzzle): Long {
    savedPuzzles.add(puzzle)
    return puzzle.id
  }

  override suspend fun savePuzzles(puzzles: List<Puzzle>) {
    savedPuzzles.addAll(puzzles)
  }

  override suspend fun markAsPlayed(puzzleId: Long) {
    markedPlayed.add(puzzleId)
  }

  override suspend fun deleteUnplayedAiPuzzles() {
    unplayedAiPuzzlesCleared = true
  }

  override suspend fun markAllUnplayed() {
    allUnplayed = true
  }

  override suspend fun deleteAll() {
    allDeleted = true
    savedPuzzles.clear()
  }

  override suspend fun getById(id: Long): Puzzle? = puzzle?.takeIf { it.id == id }
}
