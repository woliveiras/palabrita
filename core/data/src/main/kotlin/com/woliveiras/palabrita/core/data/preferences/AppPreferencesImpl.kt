package com.woliveiras.palabrita.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "palabrita_prefs")

@Singleton
class AppPreferencesImpl @Inject constructor(@ApplicationContext private val context: Context) :
  AppPreferences {

  private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")
  private val generationCycleKey = intPreferencesKey("generation_cycle")

  override val isOnboardingComplete: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[onboardingCompleteKey] ?: false }

  override val generationCycle: Flow<Int> =
    context.dataStore.data.map { prefs -> prefs[generationCycleKey] ?: 0 }

  override suspend fun setOnboardingComplete() {
    context.dataStore.edit { prefs -> prefs[onboardingCompleteKey] = true }
  }

  override suspend fun incrementGenerationCycle() {
    context.dataStore.edit { prefs ->
      val current = prefs[generationCycleKey] ?: 0
      prefs[generationCycleKey] = current + 1
    }
  }
}
