package com.woliveiras.palabrita

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.woliveiras.palabrita.core.data.preferences.AppPreferences
import com.woliveiras.palabrita.ui.theme.PalabritaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var appPreferences: AppPreferences

  private var isReady = false

  override fun onCreate(savedInstanceState: Bundle?) {
    val splashScreen = installSplashScreen()
    super.onCreate(savedInstanceState)
    splashScreen.setKeepOnScreenCondition { !isReady }
    lifecycleScope.launch {
      appPreferences.isOnboardingComplete.first()
      isReady = true
    }
    enableEdgeToEdge()
    setContent { PalabritaTheme { PalabritaNavGraph(appPreferences) } }
  }
}
