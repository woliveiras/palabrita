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

@Composable
fun OnboardingScreen(
  onComplete: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: OnboardingViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

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
      OnboardingStep.GENERATION ->
        GenerationScreen(progress = state.generationProgress)
      OnboardingStep.COMPLETE -> { /* handled above */ }
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
      text = "Descubra a palavra do dia",
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(12.dp))
    Text(
      text = "Um jogo de palavras com inteligência artificial, direto no seu celular",
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(48.dp))
    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
      Text("Começar")
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
      text = "Em qual idioma você quer jogar?",
      style = MaterialTheme.typography.headlineSmall,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = "Você pode mudar isso depois nas configurações",
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
      TextButton(onClick = onBack) { Text("Voltar") }
      Button(onClick = onNext) { Text("Continuar") }
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
        text = "Seu dispositivo",
        style = MaterialTheme.typography.headlineSmall,
      )
      Spacer(Modifier.height(16.dp))
      Text(
        text = "Seu dispositivo não suporta IA local. Mas não se preocupe! " +
          "Você jogará com nosso banco de palavras que é super divertido igual!",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.weight(1f))
      Button(onClick = onSkipToLight, modifier = Modifier.fillMaxWidth()) {
        Text("Entendi, vamos jogar!")
      }
    } else {
      // Medium or High tier — model selection
      Text(
        text = "Você quer escolher sua IA?",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))

      ModelCard(
        title = "Tenho um dispositivo potente",
        subtitle = "Gemma 4 E2B · ~2,6 GB de download",
        info = "Requer 8 GB de RAM",
        isRecommended = deviceTier == DeviceTier.HIGH,
        isSelected = selectedModel == ModelId.GEMMA4_E2B,
        onClick = { onSelectModel(ModelId.GEMMA4_E2B) },
      )
      Spacer(Modifier.height(12.dp))
      ModelCard(
        title = "Preciso economizar espaço",
        subtitle = "Gemma 3 1B · ~529 MB de download",
        info = "Requer 4 GB de RAM",
        isRecommended = deviceTier == DeviceTier.MEDIUM,
        isSelected = selectedModel == ModelId.GEMMA3_1B,
        onClick = { onSelectModel(ModelId.GEMMA3_1B) },
      )

      Spacer(Modifier.height(24.dp))
      OutlinedButton(onClick = onAutoSelect, modifier = Modifier.fillMaxWidth()) {
        Text("Não, escolha pra mim")
      }

      Spacer(Modifier.weight(1f))
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onBack) { Text("Voltar") }
        Button(onClick = onNext, enabled = selectedModel != null) {
          Text("Continuar")
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
            text = "Recomendado",
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
      text = "Preparando sua IA",
      style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = "O modelo de inteligência artificial será baixado para o seu celular. " +
        "Depois disso, tudo funcionará offline!",
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    val modelName = when (modelId) {
      ModelId.GEMMA4_E2B -> "Gemma 4 E2B (~2,6 GB)"
      ModelId.GEMMA3_1B -> "Gemma 3 1B (~1 GB)"
      else -> "—"
    }
    Text(text = "Modelo: $modelName", style = MaterialTheme.typography.bodyMedium)
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
        text = errorMessage ?: "Falha no download",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))
      Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text("Tentar novamente")
      }
      Spacer(Modifier.height(8.dp))
      OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
        Text("Escolher outro modelo")
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
        "Verificando..."
      }
      Text(
        text = "$percentText — $sizeText",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(32.dp))
      OutlinedButton(onClick = onCancel) {
        Text("Cancelar")
      }
    }
  }
}

@Composable
private fun GenerationScreen(progress: GenerationProgress?) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = "Gerando seus primeiros desafios...",
      style = MaterialTheme.typography.headlineSmall,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = "Isso acontece apenas na primeira vez",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    CircularProgressIndicator(modifier = Modifier.size(48.dp))
    if (progress != null) {
      Spacer(Modifier.height(16.dp))
      Text(
        text = "Puzzle ${progress.current} de ${progress.total}...",
        style = MaterialTheme.typography.bodyMedium,
      )
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
    title = { Text("Atenção") },
    text = {
      Text(
        "O modelo selecionado pode exigir mais memória do que seu dispositivo possui. " +
          "A performance pode ser ruim ou o app pode travar."
      )
    },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text("Continuar mesmo assim") }
    },
    dismissButton = {
      TextButton(onClick = onChooseOther) { Text("Escolher outro") }
    },
  )
}

private fun formatBytes(bytes: Long): String {
  val gb = bytes / 1_000_000_000.0
  return if (gb >= 1.0) "%.1f GB".format(gb) else "%.0f MB".format(bytes / 1_000_000.0)
}

