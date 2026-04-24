package com.woliveiras.palabrita.core.testing

import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAppPreferences : AppPreferences {
  private val _isOnboardingComplete = MutableStateFlow(false)
  override val isOnboardingComplete: Flow<Boolean> = _isOnboardingComplete

  private val _generationCycle = MutableStateFlow(0)
  override val generationCycle: Flow<Int> = _generationCycle

  override suspend fun setOnboardingComplete() {
    _isOnboardingComplete.value = true
  }

  override suspend fun incrementGenerationCycle() {
    _generationCycle.value += 1
  }

  override suspend fun resetGenerationCycle() {
    _generationCycle.value = 0
  }
}
