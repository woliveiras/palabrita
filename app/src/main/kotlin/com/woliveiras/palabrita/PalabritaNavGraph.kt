package com.woliveiras.palabrita

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.feature.chat.ChatScreen
import com.woliveiras.palabrita.feature.game.GameScreen
import com.woliveiras.palabrita.feature.home.HomeScreen
import com.woliveiras.palabrita.feature.home.HowToPlayScreen
import com.woliveiras.palabrita.feature.onboarding.GenerationScreen
import com.woliveiras.palabrita.feature.onboarding.OnboardingScreen
import com.woliveiras.palabrita.feature.settings.AiInfoScreen
import com.woliveiras.palabrita.feature.settings.SettingsScreen
import com.woliveiras.palabrita.ui.SplashScreen
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable data object SplashRoute

@Serializable data object OnboardingRoute

@Serializable data object HomeRoute

@Serializable
data class GenerationRoute(val modelId: String = "", val isRegeneration: Boolean = false)

@Serializable data object GameRoute

@Serializable data class ChatRoute(val puzzleId: Long)

@Serializable data object SettingsRoute

@Serializable data object AiInfoRoute

@Serializable data object HowToPlayRoute

@Composable
fun PalabritaNavGraph(appPreferences: AppPreferences) {
  val isOnboardingComplete: Boolean? by
    remember(appPreferences) { appPreferences.isOnboardingComplete.map { it as Boolean? } }
      .collectAsState(initial = null)
  val navController = rememberNavController()

  NavHost(
    navController = navController,
    startDestination = SplashRoute,
    enterTransition = { fadeIn(animationSpec = tween(300)) },
    exitTransition = { fadeOut(animationSpec = tween(300)) },
    popEnterTransition = { fadeIn(animationSpec = tween(300)) },
    popExitTransition = { fadeOut(animationSpec = tween(300)) },
  ) {
    composable<SplashRoute> {
      SplashScreen(
        onNavigationReady = {
          val destination = if (isOnboardingComplete == true) HomeRoute else OnboardingRoute
          navController.navigate(destination) { popUpTo(SplashRoute) { inclusive = true } }
        }
      )
    }
    composable<OnboardingRoute> {
      OnboardingScreen(
        onComplete = {
          navController.navigate(HomeRoute) { popUpTo(OnboardingRoute) { inclusive = true } }
        },
        onNavigateToGeneration = { modelId ->
          navController.navigate(GenerationRoute(modelId = modelId.name)) {
            popUpTo(OnboardingRoute) { inclusive = true }
          }
        },
      )
    }
    composable<GenerationRoute> { backStackEntry ->
      val route = backStackEntry.toRoute<GenerationRoute>()
      val modelId =
        try {
          com.woliveiras.palabrita.core.model.ModelId.valueOf(route.modelId)
        } catch (_: Exception) {
          null
        }
      GenerationScreen(
        modelId = modelId,
        isRegeneration = route.isRegeneration,
        onComplete = {
          navController.navigate(HomeRoute) { popUpTo(GenerationRoute::class) { inclusive = true } }
        },
        onCancel = {
          val destination = if (route.isRegeneration) HomeRoute else OnboardingRoute
          navController.navigate(destination) {
            popUpTo(GenerationRoute::class) { inclusive = true }
          }
        },
      )
    }
    composable<HomeRoute> {
      HomeScreen(
        onNavigateToGame = { navController.navigate(GameRoute) },
        onNavigateToGeneration = { navController.navigate(GenerationRoute(isRegeneration = true)) },
        onNavigateToSettings = { navController.navigate(SettingsRoute) },
        onNavigateToAiInfo = { navController.navigate(AiInfoRoute) },
        onNavigateToHowToPlay = { navController.navigate(HowToPlayRoute) },
      )
    }
    composable<GameRoute> {
      GameScreen(
        onNavigateToChat = { puzzleId -> navController.navigate(ChatRoute(puzzleId)) },
        onNavigateToSettings = { navController.navigate(SettingsRoute) },
        onNavigateToHome = { navController.popBackStack(HomeRoute, inclusive = false) },
        onNoPuzzlesLeft = {
          navController.navigate(GenerationRoute(isRegeneration = true)) {
            popUpTo(HomeRoute) { inclusive = false }
          }
        },
      )
    }
    composable<ChatRoute> { backStackEntry ->
      val route = backStackEntry.toRoute<ChatRoute>()
      ChatScreen(puzzleId = route.puzzleId, onBack = { navController.popBackStack() })
    }
    composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
    composable<AiInfoRoute> { AiInfoScreen(onBack = { navController.popBackStack() }) }
    composable<HowToPlayRoute> {
      HowToPlayScreen(
        onBack = { navController.popBackStack() },
        onStartPlaying = { navController.navigate(GameRoute) },
      )
    }
  }
}
