package com.woliveiras.palabrita

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.preferences.AppPreferences
import com.woliveiras.palabrita.feature.chat.ChatScreen
import com.woliveiras.palabrita.feature.game.GameScreen
import com.woliveiras.palabrita.feature.home.HomeScreen
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

private data class BottomNavItem(val route: Any, val icon: ImageVector, val labelRes: Int)

@Composable
fun PalabritaNavGraph(appPreferences: AppPreferences) {
  val isOnboardingComplete: Boolean? by
    remember(appPreferences) { appPreferences.isOnboardingComplete.map { it as Boolean? } }
      .collectAsState(initial = null)
  val navController = rememberNavController()

  val bottomNavItems =
    listOf(
      BottomNavItem(HomeRoute, Icons.Rounded.Home, CommonR.string.home_tab),
      BottomNavItem(AiInfoRoute, Icons.Rounded.SmartToy, CommonR.string.ai_tab),
      BottomNavItem(SettingsRoute, Icons.Rounded.MoreHoriz, CommonR.string.more_tab),
    )

  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  val showBottomBar = bottomNavItems.any { item ->
    currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
  }

  Scaffold(
    bottomBar = {
      if (showBottomBar) {
        NavigationBar {
          bottomNavItems.forEach { item ->
            val selected =
              currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
            NavigationBarItem(
              selected = selected,
              onClick = {
                navController.navigate(item.route) {
                  popUpTo(HomeRoute) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
              label = { Text(stringResource(item.labelRes)) },
            )
          }
        }
      }
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = SplashRoute,
      modifier = Modifier.padding(innerPadding),
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
            navController.navigate(HomeRoute) {
              popUpTo(GenerationRoute::class) { inclusive = true }
            }
          },
          onCancel = {
            navController.navigate(OnboardingRoute) {
              popUpTo(GenerationRoute::class) { inclusive = true }
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
      composable<AiInfoRoute> { AiInfoScreen() }
    }
  }
}
