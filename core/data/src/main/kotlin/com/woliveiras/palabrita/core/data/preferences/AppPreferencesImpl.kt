package com.woliveiras.palabrita.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "palabrita_prefs")

@Singleton
class AppPreferencesImpl @Inject constructor(
  @ApplicationContext private val context: Context,
) : AppPreferences {

  private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")

  override val isOnboardingComplete: Flow<Boolean> =
    context.dataStore.data.map { prefs -> prefs[onboardingCompleteKey] ?: false }

  override suspend fun setOnboardingComplete() {
    context.dataStore.edit { prefs -> prefs[onboardingCompleteKey] = true }
  }
}
