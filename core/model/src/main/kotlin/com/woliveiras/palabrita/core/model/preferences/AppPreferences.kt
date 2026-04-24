package com.woliveiras.palabrita.core.model.preferences

import kotlinx.coroutines.flow.Flow

interface AppPreferences {
  val isOnboardingComplete: Flow<Boolean>

  val generationCycle: Flow<Int>

  val appLanguage: Flow<String>

  suspend fun setOnboardingComplete()

  suspend fun incrementGenerationCycle()

  suspend fun resetGenerationCycle()

  suspend fun setAppLanguage(language: String)
}
