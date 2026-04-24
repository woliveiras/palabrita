package com.woliveiras.palabrita.feature.home

data class HomeState(
  val isLoading: Boolean = true,
  val languageDisplay: String = "",
  val unplayedCount: Int = 0,
  val isGeneratingPuzzles: Boolean = false,
  val generationComplete: Boolean = false,
  val totalPlayed: Int = 0,
  val winRate: Float = 0f,
  val currentStreak: Int = 0,
  val showHowToPlay: Boolean = false,
)
