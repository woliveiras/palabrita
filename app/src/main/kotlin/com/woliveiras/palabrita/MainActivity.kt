package com.woliveiras.palabrita

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.woliveiras.palabrita.core.model.ThemeMode
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.ui.theme.PalabritaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  @Inject lateinit var appPreferences: AppPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    // Always use light (white) status bar icons — our design always shows a dark
    // ContentPrimary frame behind the status bar regardless of light/dark theme.
    enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
    setContent {
      val themeMode by appPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
      val darkTheme =
        when (themeMode) {
          ThemeMode.LIGHT -> false
          ThemeMode.DARK -> true
          ThemeMode.SYSTEM -> false // TODO: remove — forced light for manual testing
        }
      PalabritaTheme(darkTheme = darkTheme) { PalabritaNavGraph(appPreferences, darkTheme) }
    }
  }
}
