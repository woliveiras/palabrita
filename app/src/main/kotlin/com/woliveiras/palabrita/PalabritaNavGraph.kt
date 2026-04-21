package com.woliveiras.palabrita

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
fun PalabritaNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = OnboardingRoute) {
        composable<OnboardingRoute> {
            OnboardingScreen(onComplete = { navController.navigate(GameRoute) })
        }
        composable<GameRoute> {
            GameScreen(
                onNavigateToChat = { puzzleId -> navController.navigate(ChatRoute(puzzleId)) }
            )
        }
        composable<ChatRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ChatRoute>()
            ChatScreen(puzzleId = route.puzzleId, onBack = { navController.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
