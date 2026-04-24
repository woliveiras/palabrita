package com.woliveiras.palabrita.core.model.usecase

import com.woliveiras.palabrita.core.model.repository.ChatRepository
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository

class ResetProgressUseCase(
  private val statsRepository: StatsRepository,
  private val gameSessionRepository: GameSessionRepository,
  private val chatRepository: ChatRepository,
  private val puzzleRepository: PuzzleRepository,
) {
  suspend operator fun invoke() {
    statsRepository.resetProgress()
    gameSessionRepository.deleteAll()
    chatRepository.deleteAll()
    puzzleRepository.markAllUnplayed()
  }
}
