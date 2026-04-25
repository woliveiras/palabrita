package com.woliveiras.palabrita

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.woliveiras.palabrita.core.common.PalabritaColors
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.feature.chat.ChatScreen
import com.woliveiras.palabrita.feature.game.GameScreen
import com.woliveiras.palabrita.feature.home.HomeScreen
import com.woliveiras.palabrita.feature.home.HowToPlayScreen
import com.woliveiras.palabrita.feature.onboarding.GenerationScreen
import com.woliveiras.palabrita.feature.onboarding.OnboardingScreen
import com.woliveiras.palabrita.feature.settings.AiInfoScreen
import com.woliveiras.palabrita.feature.settings.LanguageSelectionScreen
import com.woliveiras.palabrita.feature.settings.ModelDownloadScreen
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

@Serializable data object LanguageSelectionRoute

@Serializable data class ModelDownloadRoute(val modelId: String)

@Composable
fun PalabritaNavGraph(appPreferences: AppPreferences, darkTheme: Boolean) {
  val isOnboardingComplete: Boolean? by
    remember(appPreferences) { appPreferences.isOnboardingComplete.map { it as Boolean? } }
      .collectAsState(initial = null)
  val navController = rememberNavController()

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(PalabritaColors.ContentPrimary)
        .windowInsetsPadding(WindowInsets.statusBars)
        .consumeWindowInsets(WindowInsets.statusBars)
        .padding(horizontal = 12.dp)
        .padding(top = 8.dp, bottom = 12.dp)
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
    ) {
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
              if (route.isRegeneration) {
                navController.popBackStack(HomeRoute, false)
              } else {
                navController.navigate(HomeRoute) {
                  popUpTo(GenerationRoute::class) { inclusive = true }
                }
              }
            },
            onCancel = {
              if (route.isRegeneration) {
                navController.popBackStack(HomeRoute, false)
              } else {
                navController.navigate(OnboardingRoute) {
                  popUpTo(GenerationRoute::class) { inclusive = true }
                }
              }
            },
          )
        }
        composable<HomeRoute> {
          HomeScreen(
            onNavigateToGame = { navController.navigate(GameRoute) },
            onNavigateToGeneration = {
              navController.navigate(GenerationRoute(isRegeneration = true))
            },
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
        composable<SettingsRoute> {
          SettingsScreen(
            onBack = { navController.popBackStack() },
            onNavigateToModelDownload = { modelId ->
              navController.navigate(ModelDownloadRoute(modelId = modelId.name))
            },
            onNavigateToGeneration = {
              navController.navigate(GenerationRoute(isRegeneration = true))
            },
            onNavigateToLanguageSelection = { navController.navigate(LanguageSelectionRoute) },
            onNavigateToAiInfo = { navController.navigate(AiInfoRoute) },
          )
        }
        composable<AiInfoRoute> {
          AiInfoScreen(
            onBack = { navController.popBackStack() },
            onNavigateToSettings = { navController.navigate(SettingsRoute) },
          )
        }
        composable<HowToPlayRoute> {
          HowToPlayScreen(
            onBack = { navController.popBackStack() },
            onStartPlaying = { navController.navigate(GameRoute) },
          )
        }
        composable<LanguageSelectionRoute> {
          LanguageSelectionScreen(
            onBack = { navController.popBackStack() },
            onNavigateToGeneration = { _ ->
              navController.navigate(GenerationRoute(isRegeneration = true)) {
                popUpTo(LanguageSelectionRoute) { inclusive = true }
              }
            },
          )
        }
        composable<ModelDownloadRoute> { backStackEntry ->
          val route = backStackEntry.toRoute<ModelDownloadRoute>()
          ModelDownloadScreen(
            onBack = { navController.popBackStack() },
            onNavigateToGeneration = { modelId ->
              navController.navigate(
                GenerationRoute(modelId = modelId.name, isRegeneration = true)
              ) {
                popUpTo(ModelDownloadRoute::class) { inclusive = true }
              }
            },
          )
        }
      } // NavHost
    } // Surface
  } // Box
}
