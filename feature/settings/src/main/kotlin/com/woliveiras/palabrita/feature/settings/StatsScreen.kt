package com.woliveiras.palabrita.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.woliveiras.palabrita.core.common.R as CommonR
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
  onBack: () -> Unit,
  onShareStats: (String) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(CommonR.string.stats_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(CommonR.string.back))
          }
        },
      )
    },
  ) { padding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp),
    ) {
      Spacer(Modifier.height(16.dp))

      // Summary cards
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        StatCard(value = "${state.stats.totalPlayed}", label = stringResource(CommonR.string.stats_games))
        StatCard(value = "${state.winRate}%", label = stringResource(CommonR.string.stats_wins))
      }
      Spacer(Modifier.height(16.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        StatCard(value = "${state.stats.currentStreak}", label = stringResource(CommonR.string.stats_current_streak))
        StatCard(value = "${state.stats.maxStreak}", label = stringResource(CommonR.string.stats_best_streak))
      }

      Spacer(Modifier.height(32.dp))

      // Histogram
      Text(
        stringResource(CommonR.string.stats_distribution),
        style = MaterialTheme.typography.titleMedium,
      )
      Spacer(Modifier.height(12.dp))
      GuessDistributionChart(state.stats.guessDistribution)

      Spacer(Modifier.height(32.dp))

      // Share button
      OutlinedButton(
        onClick = { onShareStats(generateShareStatsText(state.stats, context)) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Icon(Icons.Rounded.Share, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(CommonR.string.stats_share))
      }

      Spacer(Modifier.height(32.dp))
    }
  }
}

@Composable
private fun StatCard(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(value, style = MaterialTheme.typography.headlineLarge)
    Text(
      label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun GuessDistributionChart(distribution: Map<Int, Int>) {
  val maxCount = distribution.values.maxOrNull() ?: 1

  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    (1..6).forEach { attempt ->
      val count = distribution[attempt] ?: 0
      val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "$attempt",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.width(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .weight(1f)
            .height(24.dp),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(fraction.coerceAtLeast(0.02f))
              .height(24.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(MaterialTheme.colorScheme.primary),
          )
        }
        Spacer(Modifier.width(8.dp))
        Text(
          text = "$count",
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.width(32.dp),
        )
      }
    }
  }
}
