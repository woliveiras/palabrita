package com.woliveiras.palabrita.core.model.preferences

import kotlinx.coroutines.flow.Flow

interface AppPreferences {
  val isOnboardingComplete: Flow<Boolean>

  val generationCycle: Flow<Int>

  suspend fun setOnboardingComplete()

  suspend fun incrementGenerationCycle()
}
