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
  val snackbarHostState = remember { SnackbarHostState() }

  var showLanguageDialog by remember { mutableStateOf(false) }
  var showWordSizeDialog by remember { mutableStateOf(false) }
  var showDeleteModelDialog by remember { mutableStateOf(false) }
  var showResetDialog by remember { mutableStateOf(false) }

  LaunchedEffect(state.error) {
    state.error?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.onAction(SettingsAction.DismissError)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Configurações") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
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
      SectionHeader("Jogo")

      ListItem(
        headlineContent = { Text("Idioma das palavras") },
        supportingContent = { Text(LANGUAGES.find { it.first == state.currentLanguage }?.second ?: state.currentLanguage) },
        leadingContent = { Icon(Icons.Rounded.Language, contentDescription = null) },
        modifier = Modifier.clickable { showLanguageDialog = true },
      )

      ListItem(
        headlineContent = { Text("Tamanho das palavras") },
        supportingContent = {
          if (state.isWordSizeUnlocked) {
            val opt = WORD_SIZE_OPTIONS.find { it.key == state.wordSizePreference }
            Text(opt?.label ?: "Padrão")
          } else {
            Text("Desbloqueado no Astuto")
          }
        },
        leadingContent = {
          if (state.isWordSizeUnlocked) {
            Icon(Icons.Rounded.SortByAlpha, contentDescription = null)
          } else {
            Icon(Icons.Rounded.Lock, contentDescription = "Bloqueado")
          }
        },
        modifier = Modifier.clickable {
          if (state.isWordSizeUnlocked) showWordSizeDialog = true
        },
      )

      ListItem(
        headlineContent = { Text("Estatísticas") },
        supportingContent = { Text("${state.stats.totalPlayed} jogos · ${state.winRate}% vitórias") },
        modifier = Modifier.clickable { onNavigateToStats() },
      )

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      // --- INTELIGÊNCIA ARTIFICIAL ---
      SectionHeader("Inteligência Artificial")

      val modelName = when (state.currentModel.modelId) {
        ModelId.GEMMA4_E2B -> "Gemma 4 E2B"
        ModelId.GEMMA3_1B -> "Gemma 3 1B"
        ModelId.NONE -> "Nenhum (Modo Light)"
      }
      ListItem(
        headlineContent = { Text("Modelo atual") },
        supportingContent = { Text(modelName) },
        leadingContent = { Icon(Icons.Rounded.Memory, contentDescription = null) },
      )

      if (state.currentModel.modelId != ModelId.NONE &&
        state.currentModel.downloadState == DownloadState.DOWNLOADED
      ) {
        ListItem(
          headlineContent = { Text("Espaço usado") },
          supportingContent = {
            val sizeMb = state.currentModel.sizeBytes / (1024 * 1024)
            Text("Modelo: ${sizeMb} MB")
          },
          leadingContent = { Icon(Icons.Rounded.Storage, contentDescription = null) },
        )
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      // --- DADOS ---
      SectionHeader("Dados")

      if (state.currentModel.modelId != ModelId.NONE) {
        ListItem(
          headlineContent = { Text("Excluir modelo") },
          supportingContent = { Text("Muda para modo Light") },
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
          Text("Resetar progresso", color = MaterialTheme.colorScheme.error)
        },
        supportingContent = { Text("Limpa stats e puzzles") },
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
      title = { Text("Excluir modelo") },
      text = {
        Text(
          "Isso excluirá o modelo de IA. O app mudará para o modo Light " +
            "com banco de palavras curado. Você pode baixar um modelo novamente a qualquer momento.",
        )
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.onAction(SettingsAction.DeleteModel)
          showDeleteModelDialog = false
        }) {
          Text("Excluir", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteModelDialog = false }) { Text("Cancelar") }
      },
    )
  }

  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text("Resetar progresso") },
      text = {
        Text(
          "Isso apagará todas as suas estatísticas, puzzles jogados e " +
            "histórico de chat. Esta ação não pode ser desfeita.",
        )
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.onAction(SettingsAction.ResetProgress)
          showResetDialog = false
        }) {
          Text("Resetar", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) { Text("Cancelar") }
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
    title = { Text("Idioma das palavras") },
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
      TextButton(onClick = onDismiss) { Text("Fechar") }
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
    title = { Text("Tamanho das palavras") },
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
              Text(option.label)
              Text(
                option.description,
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
      TextButton(onClick = onDismiss) { Text("Fechar") }
    },
  )
}
