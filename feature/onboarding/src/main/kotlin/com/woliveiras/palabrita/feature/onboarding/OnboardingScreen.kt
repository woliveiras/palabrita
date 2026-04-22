package com.woliveiras.palabrita.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.DeviceTier
import com.woliveiras.palabrita.core.model.ModelId
import androidx.compose.ui.res.stringResource
import com.woliveiras.palabrita.core.common.R as CommonR

@Composable
fun OnboardingScreen(
  onComplete: () -> Unit,
  onNavigateToGeneration: (com.woliveiras.palabrita.core.model.ModelId) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OnboardingViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  if (state.currentStep == OnboardingStep.GENERATION) {
    state.selectedModel?.let { onNavigateToGeneration(it) }
    return
  }

  if (state.currentStep == OnboardingStep.COMPLETE) {
    onComplete()
    return
  }

  AnimatedContent(
    targetState = state.currentStep,
    transitionSpec = {
      (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it } + fadeOut())
    },
    label = "onboarding-step",
  ) { step ->
    when (step) {
      OnboardingStep.WELCOME -> WelcomeScreen(onNext = { viewModel.onAction(OnboardingAction.Next) })
      OnboardingStep.LANGUAGE ->
        LanguageScreen(
          selectedLanguage = state.selectedLanguage,
          onSelectLanguage = { viewModel.onAction(OnboardingAction.SelectLanguage(it)) },
          onNext = { viewModel.onAction(OnboardingAction.Next) },
          onBack = { viewModel.onAction(OnboardingAction.Back) },
        )
      OnboardingStep.MODEL_SELECTION ->
        ModelSelectionScreen(
          deviceTier = state.deviceTier,
          selectedModel = state.selectedModel,
          showTierWarning = state.showTierWarning,
          onSelectModel = { viewModel.onAction(OnboardingAction.SelectModel(it)) },
          onAutoSelect = { viewModel.onAction(OnboardingAction.AutoSelectModel) },
          onSkipToLight = { viewModel.onAction(OnboardingAction.SkipToLightMode) },
          onDismissWarning = { viewModel.onAction(OnboardingAction.DismissTierWarning) },
          onNext = { viewModel.onAction(OnboardingAction.Next) },
          onBack = { viewModel.onAction(OnboardingAction.Back) },
        )
      OnboardingStep.DOWNLOAD ->
        DownloadScreen(
          modelId = state.selectedModel,
          downloadProgress = state.downloadProgress,
          downloadedBytes = state.downloadedBytes,
          totalBytes = state.totalBytes,
          downloadFailed = state.downloadFailed,
          errorMessage = state.downloadErrorMessage,
          onRetry = { viewModel.onAction(OnboardingAction.RetryDownload) },
          onCancel = { viewModel.onAction(OnboardingAction.CancelDownload) },
        )
      OnboardingStep.COMPLETE -> { /* handled above */ }
      OnboardingStep.GENERATION -> { /* handled above */ }
    }
  }

  if (state.showTierWarning) {
    TierWarningDialog(
      onConfirm = { viewModel.onAction(OnboardingAction.DismissTierWarning) },
      onChooseOther = {
        viewModel.onAction(OnboardingAction.DismissTierWarning)
        viewModel.onAction(OnboardingAction.AutoSelectModel)
      },
    )
  }
}

@Composable
private fun WelcomeScreen(onNext: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = Icons.Rounded.Spellcheck,
      contentDescription = null,
      modifier = Modifier.size(72.dp),
      tint = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(24.dp))
    Text(
      text = stringResource(CommonR.string.welcome_title),
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
      text = stringResource(CommonR.string.welcome_description),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(48.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(CommonR.string.welcome_start))
    }
  }
}

@Composable
private fun LanguageScreen(
  selectedLanguage: String,
  onSelectLanguage: (String) -> Unit,
  onNext: () -> Unit,
  onBack: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.height(48.dp))
    Text(
      text = stringResource(CommonR.string.language_title),
      style = MaterialTheme.typography.headlineSmall,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = stringResource(CommonR.string.language_hint),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))

    LanguageCard("PT", "Português", "pt", selectedLanguage, onSelectLanguage)
    Spacer(Modifier.height(12.dp))
    LanguageCard("EN", "English", "en", selectedLanguage, onSelectLanguage)
    Spacer(Modifier.height(12.dp))
    LanguageCard("ES", "Español", "es", selectedLanguage, onSelectLanguage)

    Spacer(Modifier.weight(1f))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      TextButton(onClick = onBack) { Text(stringResource(CommonR.string.back)) }
      Button(onClick = onNext) { Text(stringResource(CommonR.string.continue_button)) }
    }
  }
}

@Composable
private fun LanguageCard(
  flag: String,
  label: String,
  code: String,
  selected: String,
  onSelect: (String) -> Unit,
) {
  val isSelected = code == selected
  val border =
    if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
  val colors =
    if (isSelected)
      CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    else CardDefaults.outlinedCardColors()

  OutlinedCard(
    onClick = { onSelect(code) },
    modifier = Modifier.fillMaxWidth(),
    border = border ?: CardDefaults.outlinedCardBorder(),
    colors = colors,
  ) {
    Row(
      modifier = Modifier.padding(16.dp).fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Surface(
        shape = CircleShape,
        color = if (code == selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = flag,
            style = MaterialTheme.typography.labelMedium,
            color = if (code == selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Text(text = label, style = MaterialTheme.typography.titleMedium)
    }
  }
}

@Composable
private fun ModelSelectionScreen(
  deviceTier: DeviceTier,
  selectedModel: ModelId?,
  showTierWarning: Boolean,
  onSelectModel: (ModelId) -> Unit,
  onAutoSelect: () -> Unit,
  onSkipToLight: () -> Unit,
  onDismissWarning: () -> Unit,
  onNext: () -> Unit,
  onBack: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.height(48.dp))

    if (deviceTier == DeviceTier.LOW) {
      // Low tier — auto Light mode
      Text(
        text = stringResource(CommonR.string.model_device_title),
        style = MaterialTheme.typography.headlineSmall,
      )
      Spacer(Modifier.height(16.dp))
      Text(
        text = stringResource(CommonR.string.model_low_tier_message),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.weight(1f))
      Button(onClick = onSkipToLight, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(CommonR.string.model_low_tier_button))
      }
    } else {
      // Medium or High tier — model selection
      Text(
        text = stringResource(CommonR.string.model_selection_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))

      ModelCard(
        title = stringResource(CommonR.string.model_powerful_title),
        subtitle = stringResource(CommonR.string.model_powerful_subtitle),
        info = stringResource(CommonR.string.model_powerful_info),
        isRecommended = deviceTier == DeviceTier.HIGH,
        isSelected = selectedModel == ModelId.GEMMA4_E2B,
        onClick = { onSelectModel(ModelId.GEMMA4_E2B) },
      )
      Spacer(Modifier.height(12.dp))
      ModelCard(
        title = stringResource(CommonR.string.model_compact_title),
        subtitle = stringResource(CommonR.string.model_compact_subtitle),
        info = stringResource(CommonR.string.model_compact_info),
        isRecommended = deviceTier == DeviceTier.MEDIUM,
        isSelected = selectedModel == ModelId.QWEN3_0_6B,
        onClick = { onSelectModel(ModelId.QWEN3_0_6B) },
      )

      Spacer(Modifier.height(24.dp))
      OutlinedButton(onClick = onAutoSelect, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(CommonR.string.model_auto_select))
      }

      Spacer(Modifier.weight(1f))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onBack) { Text(stringResource(CommonR.string.back)) }
        Button(onClick = onNext, enabled = selectedModel != null) {
          Text(stringResource(CommonR.string.continue_button))
        }
      }
    }
  }
}

@Composable
private fun ModelCard(
  title: String,
  subtitle: String,
  info: String,
  isRecommended: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit,
) {
  val border =
    if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
  val colors =
    if (isSelected)
      CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    else CardDefaults.outlinedCardColors()

  OutlinedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    border = border ?: CardDefaults.outlinedCardBorder(),
    colors = colors,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      if (isRecommended) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary),
        ) {
          Text(
            text = stringResource(CommonR.string.recommended),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiary,
          )
        }
        Spacer(Modifier.height(8.dp))
      }
      Text(text = title, style = MaterialTheme.typography.titleMedium)
      Spacer(Modifier.height(4.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = info,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun DownloadScreen(
  modelId: ModelId?,
  downloadProgress: Float,
  downloadedBytes: Long,
  totalBytes: Long,
  downloadFailed: Boolean,
  errorMessage: String?,
  onRetry: () -> Unit,
  onCancel: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = stringResource(CommonR.string.download_title),
      style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = stringResource(CommonR.string.download_description),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    val modelName = when (modelId) {
      ModelId.GEMMA4_E2B -> stringResource(CommonR.string.download_model_gemma4)
      ModelId.QWEN3_0_6B -> stringResource(CommonR.string.download_model_qwen3)
      else -> stringResource(CommonR.string.download_model_unknown)
    }
    Text(text = stringResource(CommonR.string.download_model_label, modelName), style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(32.dp))

    if (downloadFailed) {
      Icon(
        imageVector = Icons.Rounded.ErrorOutline,
        contentDescription = null,
        modifier = Modifier.size(48.dp),
        tint = MaterialTheme.colorScheme.error,
      )
      Spacer(Modifier.height(12.dp))
      Text(
        text = errorMessage ?: stringResource(CommonR.string.download_failed),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))
      Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(CommonR.string.download_retry))
      }
      Spacer(Modifier.height(8.dp))
      OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(CommonR.string.download_choose_other))
      }
    } else {
      LinearProgressIndicator(
        progress = { downloadProgress },
        modifier = Modifier.fillMaxWidth().height(8.dp),
      )
      Spacer(Modifier.height(12.dp))
      val percentText = "${(downloadProgress * 100).toInt()}%"
      val sizeText = if (totalBytes > 0) {
        "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
      } else {
        stringResource(CommonR.string.download_checking)
      }
      Text(
        text = "$percentText — $sizeText",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(32.dp))
      OutlinedButton(onClick = onCancel) {
        Text(stringResource(CommonR.string.cancel))
      }
    }
  }
}

@Composable
private fun TierWarningDialog(
  onConfirm: () -> Unit,
  onChooseOther: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onChooseOther,
    title = { Text(stringResource(CommonR.string.warning)) },
    text = {
      Text(
        stringResource(CommonR.string.tier_warning_message)
      )
    },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(stringResource(CommonR.string.tier_warning_confirm)) }
    },
    dismissButton = {
      TextButton(onClick = onChooseOther) { Text(stringResource(CommonR.string.tier_warning_choose_other)) }
    },
  )
}

private fun formatBytes(bytes: Long): String {
  val gb = bytes / 1_000_000_000.0
  return if (gb >= 1.0) "%.1f GB".format(gb) else "%.0f MB".format(bytes / 1_000_000.0)
}

