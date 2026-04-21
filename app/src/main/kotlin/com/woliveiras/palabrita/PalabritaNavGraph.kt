package com.woliveiras.palabrita

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.woliveiras.palabrita.core.data.preferences.AppPreferences
import com.woliveiras.palabrita.feature.chat.ChatScreen
import com.woliveiras.palabrita.feature.game.GameScreen
import com.woliveiras.palabrita.feature.onboarding.OnboardingScreen
import com.woliveiras.palabrita.feature.settings.SettingsScreen
import com.woliveiras.palabrita.feature.settings.StatsScreen
import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute

@Serializable data object GameRoute

@Serializable data class ChatRoute(val puzzleId: Long)

@Serializable data object SettingsRoute

@Serializable data object StatsRoute

@Composable
fun PalabritaNavGraph(appPreferences: AppPreferences) {
  val isOnboardingComplete by appPreferences.isOnboardingComplete.collectAsState(initial = false)
  val navController = rememberNavController()
  val startDestination: Any = if (isOnboardingComplete) GameRoute else OnboardingRoute
  val context = LocalContext.current

  NavHost(navController = navController, startDestination = startDestination) {
    composable<OnboardingRoute> {
      OnboardingScreen(
        onComplete = {
          navController.navigate(GameRoute) {
            popUpTo(OnboardingRoute) { inclusive = true }
          }
        },
      )
    }
    composable<GameRoute> {
      GameScreen(
        onNavigateToChat = { puzzleId -> navController.navigate(ChatRoute(puzzleId)) },
        onNavigateToSettings = { navController.navigate(SettingsRoute) },
      )
    }
    composable<ChatRoute> { backStackEntry ->
      val route = backStackEntry.toRoute<ChatRoute>()
      ChatScreen(puzzleId = route.puzzleId, onBack = { navController.popBackStack() })
    }
    composable<SettingsRoute> {
      SettingsScreen(
        onBack = { navController.popBackStack() },
        onNavigateToStats = { navController.navigate(StatsRoute) },
      )
    }
    composable<StatsRoute> {
      StatsScreen(
        onBack = { navController.popBackStack() },
        onShareStats = { text ->
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          clipboard.setPrimaryClip(ClipData.newPlainText("Palabrita Stats", text))
        },
      )
    }
  }
}
