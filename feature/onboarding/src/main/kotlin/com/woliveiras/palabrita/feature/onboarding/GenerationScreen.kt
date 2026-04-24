package com.woliveiras.palabrita.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.PalabritaColors
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.ModelId

@Composable
fun GenerationScreen(
  modelId: ModelId?,
  isRegeneration: Boolean,
  onComplete: () -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: GenerationViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) { viewModel.triggerGeneration(modelId) }

  LaunchedEffect(state.isComplete) {
    if (state.isComplete && !isRegeneration) {
      viewModel.markOnboardingComplete()
    }
  }

  val totalPuzzles = state.progress.totalExpected
  val puzzlesGenerated = state.progress.generatedCount
  val progressFraction =
    if (totalPuzzles > 0) (puzzlesGenerated.toFloat() / totalPuzzles).coerceIn(0f, 1f) else 0f
  val isComplete = state.isComplete
  val isFailed = state.failed
  val isCancelled = state.cancelled
  val currentActivityResId = state.currentActivityResId

  // Floating particle animation
  val infiniteTransition = rememberInfiniteTransition(label = "gen_particles")
  val particle0Y by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = -18f,
      animationSpec =
        infiniteRepeatable(tween(2_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "p0y",
    )
  val particle1Y by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = -12f,
      animationSpec =
        infiniteRepeatable(tween(1_800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "p1y",
    )
  val particle0Alpha by
    infiniteTransition.animateFloat(
      initialValue = 0.4f,
      targetValue = 0.9f,
      animationSpec = infiniteRepeatable(tween(2_200, easing = LinearEasing), RepeatMode.Reverse),
      label = "p0a",
    )
  val particle1Alpha by
    infiniteTransition.animateFloat(
      initialValue = 0.6f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing), RepeatMode.Reverse),
      label = "p1a",
    )

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
          )
        )
        .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    // Icon with floating particles
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
      // Soft radial glow behind icon
      Box(
        modifier =
          Modifier.size(160.dp)
            .background(
              Brush.radialGradient(
                listOf(PalabritaColors.BrandPurple.copy(alpha = 0.15f), Color.Transparent)
              )
            )
      )

      // Floating particles (only when actively generating)
      if (!isComplete && !isFailed && !isCancelled) {
        Box(
          modifier =
            Modifier.size(8.dp)
              .offset(x = (-30).dp)
              .graphicsLayer {
                translationY = particle0Y
                alpha = particle0Alpha
              }
              .background(PalabritaColors.BrandIndigo, CircleShape)
        )
        Box(
          modifier =
            Modifier.size(6.dp)
              .offset(x = 28.dp, y = (-40).dp)
              .graphicsLayer {
                translationY = particle1Y
                alpha = particle1Alpha
              }
              .background(PalabritaColors.BrandViolet, CircleShape)
        )
      }

      // Icon container
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(96.dp)
            .background(
              Brush.linearGradient(
                listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)
              ),
              RoundedCornerShape(24.dp),
            ),
      ) {
        Icon(
          imageVector = if (isComplete) Icons.Rounded.CheckCircle else Icons.Rounded.AutoAwesome,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(48.dp),
        )
      }
    }

    Spacer(Modifier.height(24.dp))

    Text(
      text =
        if (isComplete) stringResource(CommonR.string.generation_complete_title)
        else if (isFailed) stringResource(CommonR.string.generation_failed)
        else stringResource(CommonR.string.generation_title),
      style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(8.dp))

    Text(
      text =
        if (isComplete) stringResource(CommonR.string.generation_complete_subtitle)
        else if (isFailed) "" else stringResource(CommonR.string.generation_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(32.dp))

    // Puzzle counter card
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(
            Brush.linearGradient(
              listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface)
            )
          )
          .border(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            RoundedCornerShape(24.dp),
          )
          .padding(24.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(CommonR.string.generation_puzzles_label),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // key forces recompose + subtle scale animation on each change
        key(puzzlesGenerated) {
          Text(
            text = puzzlesGenerated.toString(),
            style =
              MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = PalabritaColors.BrandIndigo,
              ),
          )
        }
      }
      Spacer(Modifier.height(12.dp))

      // Gradient progress bar
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
      ) {
        Box(
          modifier =
            Modifier.fillMaxWidth(fraction = progressFraction)
              .height(6.dp)
              .clip(RoundedCornerShape(50.dp))
              .background(
                Brush.horizontalGradient(
                  listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet)
                )
              )
        )
      }
    }

    Spacer(Modifier.height(24.dp))

    // Live activity feed
    AnimatedContent(
      targetState = currentActivityResId,
      transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
      label = "generation-activity",
      modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) { resId ->
      if (resId != null) {
        Text(
          text = stringResource(resId),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      } else {
        Spacer(Modifier.height(20.dp))
      }
    }

    Spacer(Modifier.height(8.dp))

    AnimatedVisibility(
      visible = isComplete,
      enter =
        slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(500)),
    ) {
      GradientButton(
        text = stringResource(CommonR.string.generation_continue),
        onClick = onComplete,
      )
    }

    if (isFailed) {
      GradientButton(
        text = stringResource(CommonR.string.generation_retry),
        onClick = { viewModel.triggerGeneration(modelId) },
      )
    }

    if (isCancelled && isRegeneration) {
      GradientButton(
        text = stringResource(CommonR.string.generation_regenerate),
        onClick = { viewModel.triggerGeneration(modelId) },
      )
      Spacer(Modifier.height(12.dp))
      TextButton(onClick = onCancel) {
        Text(
          text = stringResource(CommonR.string.generation_back),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (!isComplete && !isCancelled) {
      Spacer(Modifier.height(12.dp))
      TextButton(
        onClick = {
          viewModel.cancelGeneration()
          if (!isRegeneration) onCancel()
        }
      ) {
        Text(
          text =
            stringResource(
              if (isRegeneration) CommonR.string.generation_cancel_simple
              else CommonR.string.generation_cancel
            ),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun GradientButton(text: String, onClick: () -> Unit) {
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.fillMaxWidth()
        .height(56.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(
          Brush.horizontalGradient(listOf(PalabritaColors.BrandIndigo, PalabritaColors.BrandViolet))
        )
        .clickable(onClick = onClick),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
      color = Color.White,
    )
  }
}
