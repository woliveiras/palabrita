package com.woliveiras.palabrita.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.ModelId

@Composable
fun GenerationScreen(
  modelId: ModelId?,
  isRegeneration: Boolean,
  onComplete: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: GenerationViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(modelId) { modelId?.let { viewModel.triggerGeneration(it) } }

  LaunchedEffect(state.isComplete) {
    if (state.isComplete && !isRegeneration) {
      viewModel.markOnboardingComplete()
    }
  }

  Column(
    modifier = modifier.fillMaxSize().padding(32.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (state.isGenerating) {
      CircularProgressIndicator(modifier = Modifier.size(64.dp))
      Spacer(Modifier.height(24.dp))
      Text(
        text = stringResource(CommonR.string.generation_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(CommonR.string.generation_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    } else if (state.isComplete) {
      Text(
        text = stringResource(CommonR.string.generation_complete_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(CommonR.string.generation_complete_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))
      Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(CommonR.string.generation_continue))
      }
    } else if (state.failed) {
      Text(
        text = stringResource(CommonR.string.generation_failed),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))
      Button(
        onClick = { modelId?.let { viewModel.triggerGeneration(it) } },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(CommonR.string.generation_retry))
      }
    }
  }
}
