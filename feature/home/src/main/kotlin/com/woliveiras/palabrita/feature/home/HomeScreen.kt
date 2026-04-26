package com.woliveiras.palabrita.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.PalabritaColors
import com.woliveiras.palabrita.core.common.R as CommonR

@Composable
fun HomeScreen(
  onNavigateToGame: () -> Unit,
  onNavigateToGeneration: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToAiInfo: () -> Unit,
  onNavigateToHowToPlay: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  if (state.isLoading) {
    LoadingHome()
    return
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 20.dp)
  ) {
    // --- Header ---
    Header(languageDisplay = state.languageDisplay, onSettingsTap = onNavigateToSettings)

    Spacer(Modifier.height(32.dp))

    // --- Play CTA Card ---
    PlayCtaCard(
      unplayedCount = state.unplayedCount,
      onPlay = onNavigateToGame,
      onGenerate = onNavigateToGeneration,
    )

    Spacer(Modifier.height(24.dp))

    // --- Quick Stats Row ---
    QuickStatsRow(
      totalPlayed = state.totalPlayed,
      winRate = state.winRate,
      currentStreak = state.currentStreak,
    )

    Spacer(Modifier.height(24.dp))

    // --- Info Cards ---
    InfoCard(
      icon = Icons.Rounded.HelpOutline,
      title = stringResource(CommonR.string.home_how_to_play),
      subtitle = stringResource(CommonR.string.home_how_to_play_subtitle),
      onClick = onNavigateToHowToPlay,
      iconContainerColor = PalabritaColors.ContainerBlue,
      iconTint = PalabritaColors.OnContainerBlue,
    )

    Spacer(Modifier.height(12.dp))

    InfoCard(
      icon = Icons.Rounded.Info,
      title = stringResource(CommonR.string.home_about_ai),
      subtitle = stringResource(CommonR.string.home_about_ai_subtitle),
      onClick = onNavigateToAiInfo,
      iconContainerColor = PalabritaColors.ContainerPurple,
      iconTint = PalabritaColors.OnContainerPurple,
    )

    Spacer(Modifier.height(24.dp))
  }
}

// --- Header ---

@Composable
private fun Header(languageDisplay: String, onSettingsTap: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top,
  ) {
    Column {
      Text(
        text = stringResource(CommonR.string.home_title),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
      )
      if (languageDisplay.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(
          text = languageDisplay,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    val settingsCd = stringResource(CommonR.string.home_settings_cd)
    Surface(
      onClick = onSettingsTap,
      shape = RoundedCornerShape(16.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.size(48.dp).semantics { contentDescription = settingsCd },
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Icon(
          imageVector = Icons.Rounded.Settings,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(20.dp),
        )
      }
    }
  }
}

// --- Play CTA Card ---

private val GradientPurple =
  Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFA855F7)))

@Composable
private fun PlayCtaCard(
  unplayedCount: Int,
  onPlay: () -> Unit,
  onGenerate: () -> Unit,
) {
  val hasPlayable = unplayedCount > 0
  Card(
    onClick = if (hasPlayable) onPlay else onGenerate,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth().background(GradientPurple, RoundedCornerShape(24.dp)).padding(32.dp)
    ) {
      Column {
        Text(
          text =
            if (hasPlayable) stringResource(CommonR.string.home_play)
            else stringResource(CommonR.string.home_generate_more),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text =
            when {
              hasPlayable -> stringResource(CommonR.string.home_puzzles_remaining, unplayedCount)
              else -> stringResource(CommonR.string.home_no_puzzles)
            },
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.85f),
        )
      }
      Box(
        modifier =
          Modifier.align(Alignment.CenterEnd)
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.PlayArrow,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(32.dp),
        )
      }
    }
  }
}

// --- Quick Stats ---

@Composable
private fun QuickStatsRow(totalPlayed: Int, winRate: Float, currentStreak: Int) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    StatCard(
      value = "$totalPlayed",
      label = stringResource(CommonR.string.home_stats_games),
      icon = Icons.Rounded.PlayArrow,
      modifier = Modifier.weight(1f),
    )
    StatCard(
      value = "${(winRate * 100).toInt()}%",
      label = stringResource(CommonR.string.home_stats_winrate),
      icon = Icons.Rounded.EmojiEvents,
      modifier = Modifier.weight(1f),
    )
    StatCard(
      value = "$currentStreak",
      label = stringResource(CommonR.string.home_stats_streak),
      icon = Icons.Rounded.TrendingUp,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun StatCard(
  value: String,
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
      Box(
        modifier =
          Modifier.size(40.dp)
            .background(PalabritaColors.ContainerPurple, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = PalabritaColors.BrandIndigo,
          modifier = Modifier.size(20.dp),
        )
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

// --- Info Cards ---

@Composable
private fun InfoCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  iconContainerColor: Color,
  iconTint: Color,
) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier.size(48.dp).background(iconContainerColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = iconTint,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(Modifier.width(16.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

// --- Loading ---

@Composable
private fun LoadingHome() {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CircularProgressIndicator()
  }
}
