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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.DownloadState
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

  LaunchedEffect(errorMessage) {
    errorMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.onAction(SettingsAction.DismissError)
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
