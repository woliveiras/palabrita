package com.woliveiras.palabrita.feature.settings

import android.content.Intent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.GameRules
import com.woliveiras.palabrita.core.model.ModelId

private val LANGUAGES = listOf("pt" to "Português (BR)", "en" to "English", "es" to "Español")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val errorMessage = state.errorRes?.let { stringResource(it) }

  var showLanguageDialog by remember { mutableStateOf(false) }
  var showDeleteModelDialog by remember { mutableStateOf(false) }
  var showResetDialog by remember { mutableStateOf(false) }

  val context = LocalContext.current

  LaunchedEffect(errorMessage) {
    errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.onAction(SettingsAction.DismissError)
    }
  }

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is SettingsEvent.ShareText -> {
          val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
              putExtra(Intent.EXTRA_TEXT, event.text)
              type = "text/plain"
            }
          context.startActivity(Intent.createChooser(sendIntent, null))
        }
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(CommonR.string.settings)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(CommonR.string.back),
            )
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { padding ->
    Column(
      modifier = modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
    ) {
      // --- JOGO ---
      SectionHeader(stringResource(CommonR.string.settings_section_game))

      ListItem(
        headlineContent = { Text(stringResource(CommonR.string.settings_language)) },
        supportingContent = {
          Text(
            LANGUAGES.find { it.first == state.currentLanguage }?.second ?: state.currentLanguage
          )
        },
        leadingContent = { Icon(Icons.Rounded.Language, contentDescription = null) },
        modifier = Modifier.clickable { showLanguageDialog = true },
      )

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      // --- ESTATÍSTICAS ---
      SectionHeader(stringResource(CommonR.string.settings_section_stats))
      StatsSection(state = state, onShareStats = { viewModel.onAction(SettingsAction.ShareStats) })

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      // --- INTELIGÊNCIA ARTIFICIAL ---
      SectionHeader(stringResource(CommonR.string.settings_section_ai))

      val modelName =
        when (state.currentModel.modelId) {
          ModelId.GEMMA4_E4B -> stringResource(CommonR.string.settings_model_gemma4_e4b)
          ModelId.GEMMA4_E2B -> stringResource(CommonR.string.settings_model_gemma4)
          ModelId.PHI4_MINI -> stringResource(CommonR.string.settings_model_phi4_mini)
          ModelId.DEEPSEEK_R1_1_5B -> stringResource(CommonR.string.settings_model_deepseek_r1)
          ModelId.QWEN2_5_1_5B -> stringResource(CommonR.string.settings_model_qwen25_1_5b)
          ModelId.QWEN3_0_6B -> stringResource(CommonR.string.settings_model_qwen3)
          ModelId.NONE -> stringResource(CommonR.string.settings_model_none)
        }
      ListItem(
        headlineContent = { Text(stringResource(CommonR.string.settings_current_model)) },
        supportingContent = { Text(modelName) },
        leadingContent = { Icon(Icons.Rounded.Memory, contentDescription = null) },
      )

      if (
        state.currentModel.modelId != ModelId.NONE &&
          state.currentModel.downloadState == DownloadState.DOWNLOADED
      ) {
        ListItem(
          headlineContent = { Text(stringResource(CommonR.string.settings_storage)) },
          supportingContent = {
            val sizeMb = state.currentModel.sizeBytes / (1024 * 1024)
            Text(stringResource(CommonR.string.settings_storage_size, sizeMb))
          },
          leadingContent = { Icon(Icons.Rounded.Storage, contentDescription = null) },
        )
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      // --- DADOS ---
      SectionHeader(stringResource(CommonR.string.settings_section_data))

      if (state.currentModel.modelId != ModelId.NONE) {
        ListItem(
          headlineContent = { Text(stringResource(CommonR.string.settings_delete_model)) },
          supportingContent = { Text(stringResource(CommonR.string.settings_delete_model_hint)) },
          leadingContent = {
            Icon(
              Icons.Rounded.Delete,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
            )
          },
          modifier = Modifier.clickable { showDeleteModelDialog = true },
        )
      }

      ListItem(
        headlineContent = {
          Text(
            stringResource(CommonR.string.settings_reset_progress),
            color = MaterialTheme.colorScheme.error,
          )
        },
        supportingContent = { Text(stringResource(CommonR.string.settings_reset_hint)) },
        leadingContent = {
          Icon(
            Icons.Rounded.RestartAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
        },
        modifier = Modifier.clickable { showResetDialog = true },
      )

      Spacer(Modifier.height(32.dp))
    }
  }

  // --- Dialogs ---

  if (showLanguageDialog) {
    LanguageDialog(
      current = state.currentLanguage,
      onSelect = {
        viewModel.onAction(SettingsAction.ChangeLanguage(it))
        showLanguageDialog = false
      },
      onDismiss = { showLanguageDialog = false },
    )
  }

  if (showDeleteModelDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteModelDialog = false },
      title = { Text(stringResource(CommonR.string.settings_delete_model)) },
      text = { Text(stringResource(CommonR.string.settings_delete_model_message)) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.onAction(SettingsAction.DeleteModel)
            showDeleteModelDialog = false
          }
        ) {
          Text(
            stringResource(CommonR.string.settings_delete_confirm),
            color = MaterialTheme.colorScheme.error,
          )
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteModelDialog = false }) {
          Text(stringResource(CommonR.string.cancel))
        }
      },
    )
  }

  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text(stringResource(CommonR.string.settings_reset_progress)) },
      text = { Text(stringResource(CommonR.string.settings_reset_message)) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.onAction(SettingsAction.ResetProgress)
            showResetDialog = false
          }
        ) {
          Text(
            stringResource(CommonR.string.settings_reset_confirm),
            color = MaterialTheme.colorScheme.error,
          )
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) {
          Text(stringResource(CommonR.string.cancel))
        }
      },
    )
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title.uppercase(),
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
  )
}

@Composable
private fun LanguageDialog(current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(CommonR.string.settings_language)) },
    text = {
      Column {
        LANGUAGES.forEach { (code, name) ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onSelect(code) }
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text(name)
            if (code == current) {
              Text("✓", color = MaterialTheme.colorScheme.primary)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(CommonR.string.close)) }
    },
  )
}

// --- Stats Section ---

private const val STAT_PERCENT_MULTIPLIER = 100
private const val HISTOGRAM_BAR_MIN_WIDTH = 24
private const val HISTOGRAM_BAR_HEIGHT = 20

@Composable
private fun StatsSection(state: SettingsState, onShareStats: () -> Unit) {
  Column(modifier = Modifier.padding(horizontal = 16.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
      StatItem(
        value = state.stats.totalPlayed.toString(),
        label = stringResource(CommonR.string.stats_played),
      )
      StatItem(
        value = state.stats.totalWon.toString(),
        label = stringResource(CommonR.string.stats_won),
      )
      StatItem(value = "${state.winRate}%", label = stringResource(CommonR.string.stats_win_rate))
      StatItem(
        value =
          if (state.stats.totalPlayed > 0) String.format("%.1f", state.stats.avgAttempts) else "-",
        label = stringResource(CommonR.string.stats_avg_attempts),
      )
    }

    if (state.stats.guessDistribution.isNotEmpty()) {
      Spacer(Modifier.height(16.dp))
      Text(
        text = stringResource(CommonR.string.stats_guess_distribution),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(8.dp))
      GuessDistributionHistogram(distribution = state.stats.guessDistribution)
    }

    if (state.stats.totalPlayed > 0) {
      Spacer(Modifier.height(12.dp))
      TextButton(onClick = onShareStats, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text(stringResource(CommonR.string.share))
      }
    }
  }
}

@Composable
private fun StatItem(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun GuessDistributionHistogram(distribution: Map<Int, Int>) {
  val maxCount = distribution.values.maxOrNull() ?: 1
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    for (attempt in 1..GameRules.MAX_ATTEMPTS) {
      val count = distribution[attempt] ?: 0
      val fraction = if (maxCount > 0) count.toFloat() / maxCount else 0f
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = attempt.toString(),
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.width(16.dp),
          textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(8.dp))
        Box(
          modifier =
            Modifier.weight(1f)
              .height(HISTOGRAM_BAR_HEIGHT.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth(fraction.coerceAtLeast(if (count > 0) 0.05f else 0f))
                .height(HISTOGRAM_BAR_HEIGHT.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
          )
        }
        Spacer(Modifier.width(8.dp))
        Text(
          text = count.toString(),
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.width(HISTOGRAM_BAR_MIN_WIDTH.dp),
          textAlign = TextAlign.End,
        )
      }
    }
  }
}
