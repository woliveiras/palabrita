package com.woliveiras.palabrita.feature.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.R as CommonR

@Composable
fun HomeScreen(
  onNavigateToGame: () -> Unit,
  onNavigateToGeneration: () -> Unit,
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
        .windowInsetsPadding(WindowInsets.statusBars)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(CommonR.string.home_title),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(vertical = 8.dp),
    )

    Spacer(Modifier.height(16.dp))

    // Quick Stats
    QuickStatsRow(totalPlayed = state.totalPlayed, winRate = state.winRate)

    Spacer(Modifier.height(24.dp))

    // Play button
    if (state.unplayedCount > 0) {
      Button(onClick = onNavigateToGame, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(
          text = stringResource(CommonR.string.home_play),
          style = MaterialTheme.typography.titleMedium,
        )
      }
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(CommonR.string.home_puzzles_remaining, state.unplayedCount),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    } else if (!state.isGeneratingPuzzles) {
      Button(onClick = onNavigateToGeneration, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(
          text = stringResource(CommonR.string.home_generate_more),
          style = MaterialTheme.typography.titleMedium,
        )
      }
    }

    // Generation in-progress indicator
    if (state.isGeneratingPuzzles) {
      Spacer(Modifier.height(16.dp))
      GenerationIndicator()
    }

    // Generation complete dialog
    if (state.generationComplete) {
      AlertDialog(
        onDismissRequest = { viewModel.onAction(HomeAction.DismissGenerationBanner) },
        text = {
          Text(
            text = stringResource(CommonR.string.home_generation_complete),
            style = MaterialTheme.typography.bodyMedium,
          )
        },
        confirmButton = {
          TextButton(onClick = { viewModel.onAction(HomeAction.DismissGenerationBanner) }) {
            Text(text = stringResource(CommonR.string.close))
          }
        },
      )
    }

    Spacer(Modifier.height(24.dp))
  }
}

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

// --- Quick Stats ---

@Composable
private fun QuickStatsRow(totalPlayed: Int, winRate: Float) {
  Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      StatItem(value = "$totalPlayed", label = stringResource(CommonR.string.home_stats_games))
      StatItem(
        value = "${(winRate * 100).toInt()}%",
        label = stringResource(CommonR.string.home_stats_wins),
      )
    }
  }
}

@Composable
private fun StatItem(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
