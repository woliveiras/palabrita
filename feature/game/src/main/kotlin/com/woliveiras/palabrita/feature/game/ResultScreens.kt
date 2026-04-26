package com.woliveiras.palabrita.feature.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woliveiras.palabrita.core.common.PalabritaColors
import com.woliveiras.palabrita.core.common.R as CommonR
import com.woliveiras.palabrita.core.model.Puzzle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Win Screen
// ---------------------------------------------------------------------------

@Composable
internal fun WinResultScreen(
  puzzle: Puzzle?,
  attempts: List<Attempt>,
  hintsUsed: Int,
  onExplore: () -> Unit,
  onShare: () -> Unit,
  onPlayAgain: () -> Unit,
  onHome: () -> Unit,
) {
  val gradient =
    Brush.linearGradient(
      colors =
        listOf(
          PalabritaColors.ResultWinGradientStart,
          PalabritaColors.ResultWinGradientMid,
          PalabritaColors.ResultWinGradientEnd,
        ),
    )

  val contentAlpha = remember { Animatable(0f) }
  val contentOffset = remember { Animatable(50f) }
  val iconScale = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    launch {
      delay(300)
      iconScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
    }
    launch {
      delay(500)
      contentAlpha.animateTo(1f, tween(800))
    }
    launch {
      delay(500)
      contentOffset.animateTo(0f, tween(800))
    }
  }

  val attemptCount = attempts.size
  val precision = ((6 - attemptCount).coerceAtLeast(0) * 20)

  val performanceMessage =
    when {
      attemptCount == 1 -> stringResource(CommonR.string.result_perf_perfect)
      attemptCount <= 2 -> stringResource(CommonR.string.result_perf_incredible)
      attemptCount <= 3 -> stringResource(CommonR.string.result_perf_excellent)
      attemptCount <= 4 -> stringResource(CommonR.string.result_perf_very_good)
      else -> stringResource(CommonR.string.result_perf_got_it)
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(gradient)
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars),
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Spacer(Modifier.height(48.dp))

      // Trophy icon
      Box(
        modifier =
          Modifier.size(112.dp)
            .graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
      ) {
        Text(text = "\uD83C\uDFC6", fontSize = 56.sp)
      }

      Spacer(Modifier.height(24.dp))

      // Title
      Text(
        text = stringResource(CommonR.string.result_won),
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier =
          Modifier.graphicsLayer {
            alpha = contentAlpha.value
            translationY = contentOffset.value
          },
      )

      Spacer(Modifier.height(8.dp))

      // Performance message
      Text(
        text = performanceMessage,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.9f),
        modifier =
          Modifier.graphicsLayer {
            alpha = contentAlpha.value
            translationY = contentOffset.value
          },
      )

      Spacer(Modifier.height(4.dp))

      // Word reveal
      puzzle?.let {
        Text(
          text =
            "${stringResource(CommonR.string.result_word_was)} ${it.wordDisplay.uppercase()}",
          fontSize = 16.sp,
          color = Color.White.copy(alpha = 0.8f),
          fontWeight = FontWeight.Bold,
          modifier =
            Modifier.graphicsLayer {
              alpha = contentAlpha.value
              translationY = contentOffset.value
            },
        )
      }

      Spacer(Modifier.height(32.dp))

      // Stats card
      StatsCard(
        modifier =
          Modifier.graphicsLayer {
            alpha = contentAlpha.value
            translationY = contentOffset.value
          },
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          StatItem(
            icon = Icons.Rounded.Star,
            value = stringResource(CommonR.string.result_attempts_value, attemptCount),
            label = stringResource(CommonR.string.result_attempts_label),
          )
          StatItem(
            icon = Icons.Rounded.Star,
            value = "$precision%",
            label = stringResource(CommonR.string.result_precision_label),
          )
          StatItem(
            icon = Icons.Rounded.Star,
            value = "+150",
            label = stringResource(CommonR.string.result_xp_label),
          )
        }
      }

      Spacer(Modifier.height(24.dp))

      // Share button (primary)
      Button(
        onClick = onShare,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = PalabritaColors.ResultWinGradientMid,
          ),
      ) {
        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
          text = stringResource(CommonR.string.result_share),
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
        )
      }

      Spacer(Modifier.height(12.dp))

      // Play Again + Home
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GhostButton(
          text = stringResource(CommonR.string.result_play_again),
          icon = Icons.Rounded.Refresh,
          onClick = onPlayAgain,
          modifier = Modifier.weight(1f),
        )
        GhostButton(
          text = stringResource(CommonR.string.result_home),
          icon = Icons.Rounded.Home,
          onClick = onHome,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(Modifier.height(48.dp))
    }
  }
}

// ---------------------------------------------------------------------------
// Lose Screen
// ---------------------------------------------------------------------------

@Composable
internal fun LoseResultScreen(
  puzzle: Puzzle?,
  attempts: List<Attempt>,
  hintsUsed: Int,
  onExplore: () -> Unit,
  onShare: () -> Unit,
  onPlayAgain: () -> Unit,
  onHome: () -> Unit,
) {
  val gradient =
    Brush.linearGradient(
      colors =
        listOf(
          PalabritaColors.ResultLoseGradientStart,
          PalabritaColors.ResultLoseGradientMid,
          PalabritaColors.ResultLoseGradientEnd,
        ),
    )

  val contentAlpha = remember { Animatable(0f) }
  val contentOffset = remember { Animatable(50f) }
  val iconScale = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    launch {
      delay(300)
      iconScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMediumLow))
    }
    launch {
      delay(500)
      contentAlpha.animateTo(1f, tween(800))
    }
    launch {
      delay(500)
      contentOffset.animateTo(0f, tween(800))
    }
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(gradient)
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars),
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Spacer(Modifier.height(48.dp))

      // Sad emoji icon
      Box(
        modifier =
          Modifier.size(112.dp)
            .graphicsLayer { scaleX = iconScale.value; scaleY = iconScale.value }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
      ) {
        Text(text = "\uD83D\uDE14", fontSize = 56.sp)
      }

      Spacer(Modifier.height(24.dp))

      // Title
      Text(
        text = stringResource(CommonR.string.result_lost),
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier =
          Modifier.graphicsLayer {
            alpha = contentAlpha.value
            translationY = contentOffset.value
          },
      )

      Spacer(Modifier.height(8.dp))

      // Subtitle
      Text(
        text = stringResource(CommonR.string.result_lost_subtitle),
        fontSize = 16.sp,
        color = Color.White.copy(alpha = 0.9f),
        textAlign = TextAlign.Center,
        modifier =
          Modifier.graphicsLayer {
            alpha = contentAlpha.value
            translationY = contentOffset.value
          },
      )

      Spacer(Modifier.height(16.dp))

      // Word reveal badge
      puzzle?.let {
        Column(
          modifier =
            Modifier.clip(RoundedCornerShape(16.dp))
              .background(Color.White.copy(alpha = 0.3f))
              .padding(horizontal = 24.dp, vertical = 12.dp)
              .graphicsLayer {
                alpha = contentAlpha.value
                translationY = contentOffset.value
              },
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            text = "${stringResource(CommonR.string.result_word_was)}:",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f),
          )
          Text(
            text = it.wordDisplay.uppercase(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 4.sp,
          )
        }
      }

      Spacer(Modifier.height(32.dp))

      // Stats + Tips card
      StatsCard(
        modifier =
          Modifier.graphicsLayer {
            alpha = contentAlpha.value
            translationY = contentOffset.value
          },
      ) {
        // Stats row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
          StatItem(
            icon = Icons.Rounded.Star,
            value = "6/6",
            label = stringResource(CommonR.string.result_attempts_label),
          )
          StatItem(
            icon = Icons.Rounded.Star,
            value = stringResource(CommonR.string.result_streak_reset),
            label = stringResource(CommonR.string.result_streak_label),
          )
          puzzle?.let {
            StatItem(
              icon = Icons.Rounded.Star,
              value = it.wordDisplay.uppercase(),
              label = stringResource(CommonR.string.result_word_label),
            )
          }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))

        // Tips section
        Text(
          text = "\uD83D\uDCA1 ${stringResource(CommonR.string.result_tips_title)}",
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color.White,
        )
        Spacer(Modifier.height(12.dp))

        val tips =
          listOf(
            stringResource(CommonR.string.result_tip_1),
            stringResource(CommonR.string.result_tip_2),
            stringResource(CommonR.string.result_tip_3),
          )
        tips.forEach { tip ->
          Row(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(text = "•", color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text(text = tip, fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
          }
        }
      }

      Spacer(Modifier.height(24.dp))

      // Try Again button (primary)
      Button(
        onClick = onPlayAgain,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = PalabritaColors.ResultLoseGradientMid,
          ),
      ) {
        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
          text = stringResource(CommonR.string.result_try_again),
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp,
        )
      }

      Spacer(Modifier.height(12.dp))

      // Share + Home
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GhostButton(
          text = stringResource(CommonR.string.share),
          icon = Icons.Rounded.Share,
          onClick = onShare,
          modifier = Modifier.weight(1f),
        )
        GhostButton(
          text = stringResource(CommonR.string.result_home),
          icon = Icons.Rounded.Home,
          onClick = onHome,
          modifier = Modifier.weight(1f),
        )
      }

      Spacer(Modifier.height(48.dp))
    }
  }
}

// ---------------------------------------------------------------------------
// Shared components
// ---------------------------------------------------------------------------

@Composable
private fun StatsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(Color.White.copy(alpha = 0.2f))
        .padding(24.dp),
  ) {
    content()
  }
}

@Composable
private fun StatItem(icon: ImageVector, value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier =
        Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.3f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
    Spacer(Modifier.height(8.dp))
    Text(
      text = value,
      fontSize = 20.sp,
      fontWeight = FontWeight.Bold,
      color = Color.White,
    )
    Spacer(Modifier.height(2.dp))
    Text(
      text = label,
      fontSize = 12.sp,
      color = Color.White.copy(alpha = 0.8f),
    )
  }
}

@Composable
private fun GhostButton(
  text: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier.height(56.dp),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.4f)),
    colors =
      ButtonDefaults.outlinedButtonColors(
        containerColor = Color.White.copy(alpha = 0.2f),
        contentColor = Color.White,
      ),
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(8.dp))
    Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
  }
}
