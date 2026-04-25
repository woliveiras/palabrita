package com.woliveiras.palabrita.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.ai.AiModelInfo
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.DownloadState
import com.woliveiras.palabrita.core.model.ModelId
import com.woliveiras.palabrita.core.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  onBack: () -> Unit,
  onNavigateToModelDownload: (ModelId) -> Unit,
  onNavigateToGeneration: () -> Unit,
  onNavigateToLanguageSelection: () -> Unit,
  onNavigateToAiInfo: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val currentOnNavigateToModelDownload by rememberUpdatedState(onNavigateToModelDownload)
  val currentOnNavigateToGeneration by rememberUpdatedState(onNavigateToGeneration)
  val currentOnNavigateToLanguageSelection by rememberUpdatedState(onNavigateToLanguageSelection)
  val currentOnNavigateToAiInfo by rememberUpdatedState(onNavigateToAiInfo)

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is SettingsEvent.NavigateToModelDownload -> currentOnNavigateToModelDownload(event.modelId)
        is SettingsEvent.NavigateToGeneration -> currentOnNavigateToGeneration()
        is SettingsEvent.NavigateToLanguageSelection -> currentOnNavigateToLanguageSelection()
        is SettingsEvent.NavigateToAiInfo -> currentOnNavigateToAiInfo()
      }
    }
  }

  val appVersion =
    runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
      }
      .getOrDefault("1.0.0")

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
    }
  ) { padding ->
    Column(
      modifier = modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
    ) {
      SettingsSectionHeader(stringResource(CommonR.string.settings_section_ai_config))

      val modelName =
        state.availableModels.firstOrNull { it.modelId == state.currentModel.modelId }?.displayName
          ?: stringResource(CommonR.string.settings_model_none)

      SettingsRow(
        icon = { Icon(Icons.Rounded.Memory, contentDescription = null) },
        title = stringResource(CommonR.string.settings_ai_model),
        subtitle = modelName,
        onClick = { viewModel.onAction(SettingsAction.ShowModelPicker) },
      )

      HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

      SettingsRow(
        icon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
        title = stringResource(CommonR.string.settings_regenerate_puzzles),
        subtitle = stringResource(CommonR.string.settings_regenerate_puzzles_hint),
        onClick = { viewModel.onAction(SettingsAction.RegenPuzzles) },
      )

      Spacer(Modifier.height(8.dp))

      SettingsSectionHeader(stringResource(CommonR.string.settings_section_language))

      val languageLabel =
        when (state.currentLanguage) {
          "pt" -> stringResource(CommonR.string.language_pt)
          "en" -> stringResource(CommonR.string.language_en)
          "es" -> stringResource(CommonR.string.language_es)
          else -> state.currentLanguage
        }

      SettingsRow(
        icon = { Icon(Icons.Rounded.Language, contentDescription = null) },
        title = stringResource(CommonR.string.settings_language_row),
        subtitle = languageLabel,
        onClick = { viewModel.onAction(SettingsAction.NavigateToLanguageSelection) },
      )

      HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

      val themeIcon =
        when (state.themeMode) {
          ThemeMode.DARK -> Icons.Rounded.DarkMode
          ThemeMode.LIGHT -> Icons.Rounded.WbSunny
          ThemeMode.SYSTEM -> Icons.Rounded.WbSunny
        }
      val themeLabel =
        when (state.themeMode) {
          ThemeMode.DARK -> stringResource(CommonR.string.settings_theme_dark)
          ThemeMode.LIGHT -> stringResource(CommonR.string.settings_theme_light)
          ThemeMode.SYSTEM -> stringResource(CommonR.string.settings_theme_system)
        }
      SettingsRow(
        icon = { Icon(themeIcon, contentDescription = null) },
        title = stringResource(CommonR.string.settings_theme_row),
        subtitle = themeLabel,
        onClick = { viewModel.onAction(SettingsAction.ShowThemePicker) },
      )

      Spacer(Modifier.height(8.dp))

      SettingsSectionHeader(stringResource(CommonR.string.settings_section_about))

      SettingsRow(
        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
        title = stringResource(CommonR.string.settings_about_ai),
        subtitle = stringResource(CommonR.string.settings_about_ai_hint),
        onClick = { viewModel.onAction(SettingsAction.NavigateToAiInfo) },
      )

      HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

      ListItem(
        headlineContent = { Text(stringResource(CommonR.string.settings_app_version)) },
        trailingContent = {
          Text(
            text = appVersion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
      )

      Spacer(Modifier.height(32.dp))
    }
  }

  if (state.isModelPickerVisible) {
    ModelPickerBottomSheet(
      currentModelId = state.currentModel.modelId,
      currentDownloadState = state.currentModel.downloadState,
      availableModels = state.availableModels,
      deviceTier = state.deviceTier,
      onSelect = { viewModel.onAction(SettingsAction.SelectModel(it)) },
      onDismiss = { viewModel.onAction(SettingsAction.DismissModelPicker) },
    )
  }

  if (state.isThemePickerVisible) {
    ThemePickerBottomSheet(
      currentMode = state.themeMode,
      onSelect = { viewModel.onAction(SettingsAction.ChangeTheme(it)) },
      onDismiss = { viewModel.onAction(SettingsAction.DismissThemePicker) },
    )
  }
}

@Composable
private fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
  Text(
    text = title.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.SemiBold,
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
  )
}

@Composable
private fun SettingsRow(
  icon: @Composable () -> Unit,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ListItem(
    headlineContent = { Text(title) },
    supportingContent = { Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
    leadingContent = icon,
    modifier = modifier.clickable(onClick = onClick),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerBottomSheet(
  currentModelId: ModelId,
  currentDownloadState: DownloadState,
  availableModels: List<AiModelInfo>,
  deviceTier: DeviceTier,
  onSelect: (ModelId) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
      Text(
        text = stringResource(CommonR.string.model_picker_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 24.dp),
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = stringResource(CommonR.string.model_picker_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp),
      )
      Spacer(Modifier.height(16.dp))

      availableModels.forEach { model ->
        val isSelected =
          model.modelId == currentModelId && currentDownloadState == DownloadState.DOWNLOADED
        val isAboveTier =
          when (deviceTier) {
            DeviceTier.HIGH -> false
            DeviceTier.MEDIUM -> model.requiredRamMb > 8192L
          }
        ModelOptionCard(
          model = model,
          isSelected = isSelected,
          isAboveTier = isAboveTier,
          onClick = { onSelect(model.modelId) },
        )
      }

      LightModeOptionCard(
        isSelected = currentModelId == ModelId.NONE,
        onClick = { onSelect(ModelId.NONE) },
      )
    }
  }
}

@Composable
private fun ModelOptionCard(
  model: AiModelInfo,
  isSelected: Boolean,
  isAboveTier: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sizeMb = model.sizeBytes / (1024 * 1024)
  val sizeLabel = if (sizeMb >= 1024) "%.1f GB".format(sizeMb / 1024f) else "$sizeMb MB"
  val ramGb = model.requiredRamMb / 1024

  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelected) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceVariant
      ),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = model.displayName,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = "$sizeLabel · RAM: ${ramGb}GB",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isAboveTier) {
          Text(
            text = stringResource(CommonR.string.warning),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
      if (isSelected) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = stringResource(CommonR.string.model_picker_selected),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}

@Composable
private fun LightModeOptionCard(
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    onClick = onClick,
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelected) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceVariant
      ),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(CommonR.string.model_picker_none_title),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = stringResource(CommonR.string.model_picker_none_hint),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (isSelected) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = stringResource(CommonR.string.model_picker_selected),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerBottomSheet(
  currentMode: ThemeMode,
  onSelect: (ThemeMode) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
      Text(
        text = stringResource(CommonR.string.settings_theme_row),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
      )
      Spacer(Modifier.height(16.dp))

      listOf(
          ThemeMode.SYSTEM to CommonR.string.settings_theme_system,
          ThemeMode.LIGHT to CommonR.string.settings_theme_light,
          ThemeMode.DARK to CommonR.string.settings_theme_dark,
        )
        .forEach { (mode, labelRes) ->
          val isSelected = mode == currentMode
          Card(
            onClick = { onSelect(mode) },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors =
              CardDefaults.cardColors(
                containerColor =
                  if (isSelected) MaterialTheme.colorScheme.primaryContainer
                  else MaterialTheme.colorScheme.surfaceVariant
              ),
            shape = RoundedCornerShape(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
              )
              if (isSelected) {
                Icon(
                  imageVector = Icons.Rounded.CheckCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp),
                )
              }
            }
          }
        }
    }
  }
}
