package com.woliveiras.palabrita

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
    enableEdgeToEdge()
    setContent { PalabritaTheme { PalabritaNavGraph(appPreferences) } }
  }
}
