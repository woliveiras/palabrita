package com.woliveiras.palabrita.core.data.preferences

import kotlinx.coroutines.flow.Flow

interface AppPreferences {
  val isOnboardingComplete: Flow<Boolean>

  suspend fun setOnboardingComplete()
}
