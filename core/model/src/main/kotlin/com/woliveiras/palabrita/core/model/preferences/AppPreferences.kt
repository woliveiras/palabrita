package com.woliveiras.palabrita.core.model.preferences

import com.woliveiras.palabrita.core.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface AppPreferences {
  val isOnboardingComplete: Flow<Boolean>

  val generationCycle: Flow<Int>

  val appLanguage: Flow<String>

  val themeMode: Flow<ThemeMode>

  suspend fun setOnboardingComplete()

  suspend fun incrementGenerationCycle()

  suspend fun resetGenerationCycle()

  suspend fun setAppLanguage(language: String)

  suspend fun setThemeMode(mode: ThemeMode)
}
