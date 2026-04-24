package com.woliveiras.palabrita.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.woliveiras.palabrita.core.model.GameRules

@Composable
fun HomeScreen(
  onNavigateToGame: () -> Unit,
  onNavigateToGeneration: () -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToAiInfo: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  if (state.isLoading) {
    LoadingHome()
    return
  }

  if (state.generationComplete) {
    GenerationSuccessScreen(
      puzzleCount = state.generatedPuzzleCount,
      onStartPlaying = {
        viewModel.onAction(HomeAction.DismissGenerationBanner)
        onNavigateToGame()
      },
    )
    return
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp)
  ) {
    // --- Header ---
    Header(languageDisplay = state.languageDisplay, onSettingsTap = onNavigateToSettings)

    Spacer(Modifier.height(24.dp))

    // --- Play CTA Card ---
    PlayCtaCard(
      unplayedCount = state.unplayedCount,
      isGenerating = state.isGeneratingPuzzles,
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
      onClick = { viewModel.onAction(HomeAction.OpenHowToPlay) },
    )

    Spacer(Modifier.height(12.dp))

    InfoCard(
      icon = Icons.Rounded.Info,
      title = stringResource(CommonR.string.home_about_ai),
      subtitle = stringResource(CommonR.string.home_about_ai_subtitle),
      onClick = onNavigateToAiInfo,
    )

    // --- Generation indicator ---
    if (state.isGeneratingPuzzles) {
      Spacer(Modifier.height(16.dp))
      GenerationIndicator()
    }

    // --- How to Play dialog ---
    if (state.showHowToPlay) {
      AlertDialog(
        onDismissRequest = { viewModel.onAction(HomeAction.DismissHowToPlay) },
        title = { Text(stringResource(CommonR.string.home_rules_title)) },
        text = {
          Text(
            stringResource(CommonR.string.home_rules_body, GameRules.MAX_ATTEMPTS),
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        confirmButton = {
          TextButton(onClick = { viewModel.onAction(HomeAction.DismissHowToPlay) }) {
            Text(stringResource(CommonR.string.close))
          }
        },
      )
    }

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
    IconButton(
      onClick = onSettingsTap,
      modifier = Modifier.semantics { contentDescription = settingsCd },
    ) {
      Icon(
        imageVector = Icons.Rounded.Settings,
        contentDescription = null,
        modifier = Modifier.size(28.dp),
      )
    }
  }
}

// --- Play CTA Card ---

private val GradientPurple = Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFFC084FC)))

@Composable
private fun PlayCtaCard(
  unplayedCount: Int,
  isGenerating: Boolean,
  onPlay: () -> Unit,
  onGenerate: () -> Unit,
) {
  val hasPlayable = unplayedCount > 0
  Card(
    onClick = if (hasPlayable) onPlay else onGenerate,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth().background(GradientPurple, RoundedCornerShape(20.dp)).padding(24.dp)
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
              isGenerating -> stringResource(CommonR.string.home_generating)
              else -> stringResource(CommonR.string.home_no_puzzles)
            },
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.85f),
        )
      }
      Box(
        modifier =
          Modifier.align(Alignment.CenterEnd)
            .size(56.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.25f)),
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
      modifier = Modifier.weight(1f),
    )
    StatCard(
      value = "${(winRate * 100).toInt()}%",
      label = stringResource(CommonR.string.home_stats_winrate),
      modifier = Modifier.weight(1f),
    )
    StatCard(
      value = "$currentStreak",
      label = stringResource(CommonR.string.home_stats_streak),
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Column(
      modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
private fun InfoCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
  OutlinedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
  ) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
        }
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

// --- Generation Indicator ---

@Composable
private fun GenerationIndicator() {
  OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
      Spacer(Modifier.width(8.dp))
      Text(
        text = stringResource(CommonR.string.home_generating),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
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

// --- Generation Success ---

@Composable
private fun GenerationSuccessScreen(puzzleCount: Int, onStartPlaying: () -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.size(96.dp)
          .background(
            Brush.linearGradient(listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)),
            RoundedCornerShape(24.dp),
          ),
    ) {
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(48.dp),
      )
    }

    Spacer(Modifier.height(32.dp))

    Text(
      text = stringResource(CommonR.string.home_generation_success_title),
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(8.dp))

    Text(
      text = stringResource(CommonR.string.home_generation_success_subtitle, puzzleCount),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(32.dp))

    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.fillMaxWidth()
          .height(56.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(
            Brush.horizontalGradient(
              listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)
            )
          )
          .clickable(onClick = onStartPlaying),
    ) {
      Text(
        text = stringResource(CommonR.string.home_generation_success_cta),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
      )
    }
  }
}
