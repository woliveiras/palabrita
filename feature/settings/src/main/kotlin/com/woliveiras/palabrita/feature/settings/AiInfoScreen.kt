package com.woliveiras.palabrita.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.ai.EngineState
import com.woliveiras.palabrita.core.common.R as CommonR

@Composable
fun AiInfoScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: AiInfoViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  if (state.isLoading) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      CircularProgressIndicator()
    }
    return
  }

  Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
    // Back button
    IconButton(onClick = onBack) {
      Icon(
        Icons.AutoMirrored.Rounded.ArrowBack,
        contentDescription = stringResource(CommonR.string.generation_back),
      )
    }

    Spacer(Modifier.height(8.dp))

    // Header
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        Icons.Rounded.SmartToy,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp),
      )
      Spacer(Modifier.width(8.dp))
      Text(
        text = stringResource(CommonR.string.ai_info_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
    }

    Spacer(Modifier.height(16.dp))

    // Model Card
    ModelInfoCard(state)

    Spacer(Modifier.height(16.dp))

    // Engine Status
    EngineStatusCard(state.engineState)

    Spacer(Modifier.height(16.dp))

    // Prompts
    PromptCard(
      title = stringResource(CommonR.string.ai_info_system_prompt),
      content = state.puzzleSystemPrompt,
    )

    Spacer(Modifier.height(12.dp))

    PromptCard(
      title = stringResource(CommonR.string.ai_info_puzzle_prompt),
      content = state.puzzleSamplePrompt,
    )

    Spacer(Modifier.height(12.dp))

    PromptCard(
      title = stringResource(CommonR.string.ai_info_chat_prompt),
      content = state.chatSamplePrompt,
    )

    Spacer(Modifier.height(24.dp))
  }
}

@Composable
private fun ModelInfoCard(state: AiInfoState) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(CommonR.string.ai_info_model_section),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(8.dp))

      val info = state.modelInfo
      if (info != null) {
        InfoRow(stringResource(CommonR.string.ai_info_model_name), info.displayName)
        InfoRow(stringResource(CommonR.string.ai_info_model_size), formatBytes(info.sizeBytes))
        InfoRow(stringResource(CommonR.string.ai_info_model_ram), "${info.requiredRamMb / 1024} GB")
        InfoRow(stringResource(CommonR.string.ai_info_model_file), info.fileName)
      } else {
        Text(
          text = stringResource(CommonR.string.ai_info_no_model),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
      }
    }
  }
}

@Composable
private fun EngineStatusCard(engineState: EngineState) {
  val (icon, label, color) =
    when (engineState) {
      is EngineState.Ready ->
        Triple(
          Icons.Rounded.CheckCircle,
          stringResource(CommonR.string.ai_info_engine_ready),
          MaterialTheme.colorScheme.primary,
        )
      is EngineState.Initializing ->
        Triple(
          Icons.Rounded.HourglassTop,
          stringResource(CommonR.string.ai_info_engine_loading),
          MaterialTheme.colorScheme.tertiary,
        )
      is EngineState.Error ->
        Triple(
          Icons.Rounded.ErrorOutline,
          stringResource(CommonR.string.ai_info_engine_error),
          MaterialTheme.colorScheme.error,
        )
      is EngineState.Uninitialized ->
        Triple(
          Icons.Rounded.HourglassTop,
          stringResource(CommonR.string.ai_info_engine_off),
          MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

  OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
      Spacer(Modifier.width(12.dp))
      Column {
        Text(
          text = stringResource(CommonR.string.ai_info_engine_section),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = color)
      }
    }
  }
}

@Composable
private fun PromptCard(title: String, content: String) {
  OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
      Spacer(Modifier.height(4.dp))
      Text(
        text = content,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
  }
}

private fun formatBytes(bytes: Long): String {
  val gb = bytes / 1_000_000_000.0
  return if (gb >= 1.0) "%.1f GB".format(gb) else "%.0f MB".format(bytes / 1_000_000.0)
}
