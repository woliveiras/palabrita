package com.woliveiras.palabrita.feature.home

data class HomeState(
  val totalPlayed: Int = 0,
  val winRate: Float = 0f,
  val unplayedCount: Int = 0,
  val isGeneratingPuzzles: Boolean = false,
  val generationComplete: Boolean = false,
  val isLoading: Boolean = true,
)
