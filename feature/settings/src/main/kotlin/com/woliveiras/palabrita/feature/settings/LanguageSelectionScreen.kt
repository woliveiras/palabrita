package com.woliveiras.palabrita.feature.settings

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.R as CommonR

private val APP_LANGUAGES =
  listOf(
    "pt" to CommonR.string.language_pt,
    "en" to CommonR.string.language_en,
    "es" to CommonR.string.language_es,
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
  onBack: () -> Unit,
  onNavigateToGeneration: (String) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: LanguageSelectionViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val currentOnNavigateToGeneration by rememberUpdatedState(onNavigateToGeneration)
  val currentOnBack by rememberUpdatedState(onBack)

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is LanguageSelectionEvent.RestartForLocale -> {
          val localeList = LocaleListCompat.forLanguageTags(event.language)
          androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)
        }
        is LanguageSelectionEvent.NavigateToGeneration ->
          currentOnNavigateToGeneration(event.language)
        is LanguageSelectionEvent.NavigateBack -> currentOnBack()
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(CommonR.string.language_selection_title)) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              Icons.AutoMirrored.Rounded.ArrowBack,
              contentDescription = stringResource(CommonR.string.back),
            )
          }
        },
      )
    }
  ) { padding ->
    Column(
      modifier =
        modifier
          .fillMaxSize()
          .padding(padding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp)
    ) {
      Spacer(modifier = Modifier.height(8.dp))

      LanguageSectionHeader(stringResource(CommonR.string.language_selection_app_section))
      APP_LANGUAGES.forEach { (code, nameRes) ->
        LanguageRadioRow(
          label = stringResource(nameRes),
          selected = state.appLanguage == code,
          onClick = { viewModel.onAction(LanguageSelectionAction.ChangeAppLanguage(code)) },
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      LanguageSectionHeader(stringResource(CommonR.string.language_selection_game_section))
      state.gameLanguages.forEach { info ->
        LanguageRadioRow(
          label = "${info.flag} ${info.displayName}",
          selected = state.gameLanguage == info.code,
          onClick = { viewModel.onAction(LanguageSelectionAction.ChangeGameLanguage(info.code)) },
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  if (state.showConfirmDialog) {
    val pendingLang = state.pendingGameLanguage ?: ""
    val pendingLangName =
      state.gameLanguages.firstOrNull { it.code == pendingLang }?.displayName ?: pendingLang

    AlertDialog(
      onDismissRequest = { viewModel.onAction(LanguageSelectionAction.DismissDialog) },
      title = { Text(stringResource(CommonR.string.language_selection_confirm_title)) },
      text = {
        Text(stringResource(CommonR.string.language_selection_confirm_body, pendingLangName))
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.onAction(LanguageSelectionAction.ConfirmGameLanguage(generateNow = true))
          }
        ) {
          Text(stringResource(CommonR.string.language_selection_generate_now))
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            viewModel.onAction(LanguageSelectionAction.ConfirmGameLanguage(generateNow = false))
          }
        ) {
          Text(stringResource(CommonR.string.language_selection_generate_later))
        }
      },
    )
  }
}

@Composable
private fun LanguageSectionHeader(title: String, modifier: Modifier = Modifier) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.SemiBold,
    modifier = modifier.padding(vertical = 8.dp),
  )
}

@Composable
private fun LanguageRadioRow(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.padding(vertical = 4.dp),
  ) {
    RadioButton(selected = selected, onClick = onClick)
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(start = 8.dp),
    )
  }
}
