package com.woliveiras.palabrita

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.woliveiras.palabrita.core.data.preferences.AppPreferences
import com.woliveiras.palabrita.feature.chat.ChatScreen
import com.woliveiras.palabrita.feature.game.GameScreen
import com.woliveiras.palabrita.feature.onboarding.OnboardingScreen
import com.woliveiras.palabrita.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute

@Serializable data object GameRoute

@Serializable data class ChatRoute(val puzzleId: Long)

@Serializable data object SettingsRoute

@Composable
fun PalabritaNavGraph(appPreferences: AppPreferences) {
  val isOnboardingComplete by appPreferences.isOnboardingComplete.collectAsState(initial = false)
  val navController = rememberNavController()
  val startDestination: Any = if (isOnboardingComplete) GameRoute else OnboardingRoute

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
      GameScreen(onNavigateToChat = { puzzleId -> navController.navigate(ChatRoute(puzzleId)) })
    }
    composable<ChatRoute> { backStackEntry ->
      val route = backStackEntry.toRoute<ChatRoute>()
      ChatScreen(puzzleId = route.puzzleId, onBack = { navController.popBackStack() })
    }
    composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
  }
}
