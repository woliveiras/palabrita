package com.woliveiras.palabrita.feature.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
      Spacer(Modifier.height(32.dp))
      GenerationChecklist(steps = state.steps, modifier = Modifier.fillMaxWidth())
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

@Composable
private fun GenerationChecklist(steps: List<GenerationStep>, modifier: Modifier = Modifier) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
    steps.forEach { step -> GenerationStepRow(step) }
  }
}

@Composable
private fun GenerationStepRow(step: GenerationStep) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.animateContentSize(),
  ) {
    StepIcon(step.status)

    val textColor =
      when (step.status) {
        StepStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
      }

    Text(
      text =
        buildString {
          append(stringResource(step.labelResId))
          if (step.detail != null) {
            append(" · ")
            append(step.detail)
          }
        },
      style = MaterialTheme.typography.bodyLarge,
      color = textColor,
    )
  }
}

@Composable
private fun StepIcon(status: StepStatus) {
  when (status) {
    StepStatus.COMPLETED ->
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
      )
    StepStatus.IN_PROGRESS ->
      CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    StepStatus.PENDING ->
      Box(
        modifier =
          Modifier.size(24.dp)
            .padding(2.dp)
            .border(
              width = 2.dp,
              color = MaterialTheme.colorScheme.outlineVariant,
              shape = CircleShape,
            )
      )
  }
}
