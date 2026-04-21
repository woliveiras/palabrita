package com.woliveiras.palabrita.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SortByAlpha
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.ModelId
import androidx.compose.ui.res.stringResource
import com.woliveiras.palabrita.core.common.R as CommonR

private val LANGUAGES = listOf(
  "pt" to "Português (BR)",
  "en" to "English",
  "es" to "Español",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  onNavigateToStats: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = androidx.compose.ui.platform.LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }

  var showLanguageDialog by remember { mutableStateOf(false) }
  var showWordSizeDialog by remember { mutableStateOf(false) }
  var showDeleteModelDialog by remember { mutableStateOf(false) }
  var showResetDialog by remember { mutableStateOf(false) }

  LaunchedEffect(state.errorRes) {
    state.errorRes?.let {
      snackbarHostState.showSnackbar(context.getString(it))
      viewModel.onAction(SettingsAction.DismissError)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(CommonR.string.settings)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(CommonR.string.back))
          }
        },
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { padding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState()),
    ) {
      // --- JOGO ---
      SectionHeader(stringResource(CommonR.string.settings_section_game))

      ListItem(
        headlineContent = { Text(stringResource(CommonR.string.settings_language)) },
        supportingContent = { Text(LANGUAGES.find { it.first == state.currentLanguage }?.second ?: state.currentLanguage) },
        leadingContent = { Icon(Icons.Rounded.Language, contentDescription = null) },
        modifier = Modifier.clickable { showLanguageDialog = true },
      )

      ListItem(
        headlineContent = { Text(stringResource(CommonR.string.settings_word_size)) },
        supportingContent = {
          if (state.isWordSizeUnlocked) {
            val opt = WORD_SIZE_OPTIONS.find { it.key == state.wordSizePreference }
            Text(opt?.let { stringResource(it.labelRes) } ?: stringResource(CommonR.string.settings_word_size_default))
          } else {
            Text(stringResource(CommonR.string.settings_word_size_locked))
          }
        },
        leadingContent = {
          if (state.isWordSizeUnlocked) {
            Icon(Icons.Rounded.SortByAlpha, contentDescription = null)
          } else {
            Icon(Icons.Rounded.Lock, contentDescription = stringResource(CommonR.string.settings_word_size_locked))
          }
        },
        modifier = Modifier.clickable {
          if (state.isWordSizeUnlocked) showWordSizeDialog = true
        },
      )

      ListItem(
        headlineContent = { Text(stringResource(CommonR.string.settings_stats)) },
        supportingContent = { Text(stringResource(CommonR.string.settings_stats_summary, state.stats.totalPlayed, state.winRate)) },
        modifier = Modifier.clickable { onNavigateToStats() },
      )

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      // --- INTELIGÊNCIA ARTIFICIAL ---
      SectionHeader(stringResource(CommonR.string.settings_section_ai))

      val modelName = when (state.currentModel.modelId) {
        ModelId.GEMMA4_E2B -> stringResource(CommonR.string.settings_model_gemma4)
        ModelId.GEMMA3_1B -> stringResource(CommonR.string.settings_model_gemma3)
        ModelId.NONE -> stringResource(CommonR.string.settings_model_none)
      }
      ListItem(
        headlineContent = { Text(stringResource(CommonR.string.settings_current_model)) },
        supportingContent = { Text(modelName) },
        leadingContent = { Icon(Icons.Rounded.Memory, contentDescription = null) },
      )

      if (state.currentModel.modelId != ModelId.NONE &&
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
          Text(stringResource(CommonR.string.settings_reset_progress), color = MaterialTheme.colorScheme.error)
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

  if (showWordSizeDialog) {
    WordSizeDialog(
      current = state.wordSizePreference,
      isEpicAvailable = state.isEpicWordSizeAvailable,
      onSelect = {
        viewModel.onAction(SettingsAction.ChangeWordSize(it))
        showWordSizeDialog = false
      },
      onDismiss = { showWordSizeDialog = false },
    )
  }

  if (showDeleteModelDialog) {
    AlertDialog(
      onDismissRequest = { showDeleteModelDialog = false },
      title = { Text(stringResource(CommonR.string.settings_delete_model)) },
      text = {
        Text(
          stringResource(CommonR.string.settings_delete_model_message),
        )
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.onAction(SettingsAction.DeleteModel)
          showDeleteModelDialog = false
        }) {
          Text(stringResource(CommonR.string.settings_delete_confirm), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteModelDialog = false }) { Text(stringResource(CommonR.string.cancel)) }
      },
    )
  }

  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text(stringResource(CommonR.string.settings_reset_progress)) },
      text = {
        Text(
          stringResource(CommonR.string.settings_reset_message),
        )
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.onAction(SettingsAction.ResetProgress)
          showResetDialog = false
        }) {
          Text(stringResource(CommonR.string.settings_reset_confirm), color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) { Text(stringResource(CommonR.string.cancel)) }
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
private fun LanguageDialog(
  current: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(CommonR.string.settings_language)) },
    text = {
      Column {
        LANGUAGES.forEach { (code, name) ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
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

@Composable
private fun WordSizeDialog(
  current: String,
  isEpicAvailable: Boolean,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val options = if (isEpicAvailable) WORD_SIZE_OPTIONS else WORD_SIZE_OPTIONS.filter { it.key != "EPIC" }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(CommonR.string.settings_word_size)) },
    text = {
      Column {
        options.forEach { option ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(option.key) }
              .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(stringResource(option.labelRes))
              Text(
                stringResource(option.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (option.key == current) {
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
