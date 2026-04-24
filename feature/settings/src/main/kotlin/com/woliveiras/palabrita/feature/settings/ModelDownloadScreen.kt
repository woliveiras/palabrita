package com.woliveiras.palabrita.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.ModelId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(
  onBack: () -> Unit,
  onNavigateToGeneration: (ModelId) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ModelDownloadViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val currentOnBack by rememberUpdatedState(onBack)
  val currentOnNavigateToGeneration by rememberUpdatedState(onNavigateToGeneration)

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is ModelDownloadUiEvent.NavigateBack -> currentOnBack()
        is ModelDownloadUiEvent.NavigateToGeneration -> currentOnNavigateToGeneration(event.modelId)
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(CommonR.string.model_download_title)) },
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
      modifier = modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      when {
        state.isComplete ->
          DownloadCompleteContent(
            onBack = onBack,
            onGeneratePuzzles = { viewModel.onAction(ModelDownloadUiAction.StartDownload) },
          )
        state.errorMessage != null ->
          DownloadErrorContent(
            errorMessage = state.errorMessage!!,
            onRetry = { viewModel.onAction(ModelDownloadUiAction.StartDownload) },
            onCancel = { viewModel.onAction(ModelDownloadUiAction.CancelDownload) },
          )
        state.isDownloading ->
          DownloadingContent(
            modelName = state.modelInfo?.displayName ?: "",
            progress = state.downloadProgress,
            downloadedBytes = state.downloadedBytes,
            totalBytes = state.totalBytes,
            onCancel = { viewModel.onAction(ModelDownloadUiAction.CancelDownload) },
          )
        else ->
          DownloadIdleContent(
            modelName = state.modelInfo?.displayName ?: "",
            sizeBytes = state.modelInfo?.sizeBytes ?: 0L,
            onStart = { viewModel.onAction(ModelDownloadUiAction.StartDownload) },
            onCancel = { viewModel.onAction(ModelDownloadUiAction.CancelDownload) },
          )
      }
    }
  }
}

@Composable
private fun DownloadIdleContent(
  modelName: String,
  sizeBytes: Long,
  onStart: () -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = modelName,
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = formatSize(sizeBytes),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(CommonR.string.model_download_start))
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(CommonR.string.model_download_cancel))
    }
  }
}

@Composable
private fun DownloadingContent(
  modelName: String,
  progress: Float,
  downloadedBytes: Long,
  totalBytes: Long,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    CircularProgressIndicator(
      progress = { progress },
      modifier = Modifier.size(80.dp),
      strokeWidth = 6.dp,
    )
    Spacer(Modifier.height(24.dp))
    Text(
      text = stringResource(CommonR.string.model_download_downloading, (progress * 100).toInt()),
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = "${formatSize(downloadedBytes)} / ${formatSize(totalBytes)}",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(24.dp))
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(CommonR.string.model_download_cancel))
    }
  }
}

@Composable
private fun DownloadCompleteContent(
  onBack: () -> Unit,
  onGeneratePuzzles: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(
      imageVector = Icons.Rounded.CheckCircle,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(72.dp),
    )
    Spacer(Modifier.height(16.dp))
    Text(
      text = stringResource(CommonR.string.model_download_success),
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun DownloadErrorContent(
  errorMessage: String,
  onRetry: () -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = stringResource(CommonR.string.model_download_error_prefix, errorMessage),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.error,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(CommonR.string.model_download_retry))
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(CommonR.string.model_download_cancel))
    }
  }
}

private fun formatSize(bytes: Long): String {
  val mb = bytes / (1024 * 1024)
  return if (mb >= 1024) "%.1f GB".format(mb / 1024f) else "$mb MB"
}
