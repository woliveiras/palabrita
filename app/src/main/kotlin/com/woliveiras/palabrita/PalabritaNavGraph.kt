package com.woliveiras.palabrita

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.woliveiras.palabrita.core.data.preferences.AppPreferences
import com.woliveiras.palabrita.feature.chat.ChatScreen
import com.woliveiras.palabrita.feature.game.GameScreen
import com.woliveiras.palabrita.feature.home.HomeScreen
import com.woliveiras.palabrita.feature.onboarding.OnboardingScreen
import com.woliveiras.palabrita.feature.settings.SettingsScreen
import com.woliveiras.palabrita.feature.settings.StatsScreen
import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute

@Serializable data object HomeRoute

@Serializable
data class GameRoute(
  val dailyChallengeIndex: Int = -1,
  val dailyChallengeDifficulty: Int = -1,
)

@Serializable data class ChatRoute(val puzzleId: Long)

@Serializable data object SettingsRoute

@Serializable data object StatsRoute

private data class BottomNavItem(
  val route: Any,
  val icon: ImageVector,
  val label: String,
)

@Composable
fun PalabritaNavGraph(appPreferences: AppPreferences) {
  val isOnboardingComplete by appPreferences.isOnboardingComplete.collectAsState(initial = false)
  val navController = rememberNavController()
  val startDestination: Any = if (isOnboardingComplete) HomeRoute else OnboardingRoute
  val context = LocalContext.current

  val bottomNavItems = listOf(
    BottomNavItem(HomeRoute, Icons.Rounded.Home, "Home"),
    BottomNavItem(StatsRoute, Icons.Rounded.QueryStats, "Stats"),
    BottomNavItem(SettingsRoute, Icons.Rounded.MoreHoriz, "Mais"),
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
            val selected = currentDestination?.hierarchy?.any {
              it.hasRoute(item.route::class)
            } == true
            NavigationBarItem(
              selected = selected,
              onClick = {
                navController.navigate(item.route) {
                  popUpTo(HomeRoute) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = { Icon(item.icon, contentDescription = item.label) },
              label = { Text(item.label) },
            )
          }
        }
      }
    },
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = startDestination,
      modifier = Modifier.padding(innerPadding),
    ) {
      composable<OnboardingRoute> {
        OnboardingScreen(
          onComplete = {
            navController.navigate(HomeRoute) {
              popUpTo(OnboardingRoute) { inclusive = true }
            }
          },
        )
      }
      composable<HomeRoute> {
        HomeScreen(
          onNavigateToGame = { dailyIndex, difficulty ->
            navController.navigate(
              GameRoute(
                dailyChallengeIndex = dailyIndex ?: -1,
                dailyChallengeDifficulty = difficulty ?: -1,
              ),
            )
          },
          onNavigateToFreePlay = { navController.navigate(GameRoute()) },
          onNavigateToChat = { puzzleId -> navController.navigate(ChatRoute(puzzleId)) },
          onNavigateToStats = {
            navController.navigate(StatsRoute) {
              popUpTo(HomeRoute) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
          onNavigateToSettings = {
            navController.navigate(SettingsRoute) {
              popUpTo(HomeRoute) { saveState = true }
              launchSingleTop = true
              restoreState = true
            }
          },
        )
      }
      composable<GameRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<GameRoute>()
        GameScreen(
          dailyChallengeIndex = if (route.dailyChallengeIndex >= 0) route.dailyChallengeIndex else null,
          dailyChallengeDifficulty = if (route.dailyChallengeDifficulty >= 0) route.dailyChallengeDifficulty else null,
          onNavigateToChat = { puzzleId -> navController.navigate(ChatRoute(puzzleId)) },
          onNavigateToSettings = { navController.navigate(SettingsRoute) },
          onNavigateToHome = { navController.popBackStack(HomeRoute, inclusive = false) },
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
}
