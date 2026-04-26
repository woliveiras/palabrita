package com.woliveiras.palabrita.core.model.usecase

import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.core.model.repository.GameSessionRepository
import com.woliveiras.palabrita.core.model.repository.PuzzleRepository
import com.woliveiras.palabrita.core.model.repository.StatsRepository

class ResetProgressUseCase(
  private val statsRepository: StatsRepository,
  private val gameSessionRepository: GameSessionRepository,
  private val puzzleRepository: PuzzleRepository,
  private val appPreferences: AppPreferences,
) {
  suspend operator fun invoke() {
    statsRepository.resetProgress()
    gameSessionRepository.deleteAll()
    puzzleRepository.deleteAll()
    appPreferences.resetGenerationCycle()
  }
}
