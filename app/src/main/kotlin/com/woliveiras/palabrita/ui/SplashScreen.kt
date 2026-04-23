package com.woliveiras.palabrita.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woliveiras.palabrita.R
import com.woliveiras.palabrita.core.common.PalabritaColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Animated splash screen shown at app start while navigation destination is determined.
 *
 * Animations (translated 1:1 from the Figma/Framer Motion prototype):
 * - Logo container: scale 0.8→1 + opacity 0→1 (600ms, ease-out)
 * - Icon: gentle infinite rock ±5° (1s/cycle, ease-in-out)
 * - Title: slide-up + fade-in (delay 200ms, 500ms)
 * - Subtitle: slide-up + fade-in (delay 400ms, 500ms)
 * - Dots: fade-in at 1 000ms, then each pulses opacity [0.3→1→0.3] staggered 200ms
 *
 * Navigation: after 2 500ms calls [onNavigationReady]; caller decides the destination.
 */
@Composable
fun SplashScreen(onNavigationReady: () -> Unit) {
  LaunchedEffect(Unit) {
    delay(2_500)
    onNavigationReady()
  }

  val density = LocalDensity.current
  val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")

  // ── Infinite: icon rock ──────────────────────────────────────────────────
  val iconRotation by
    infiniteTransition.animateFloat(
      initialValue = -5f,
      targetValue = 5f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(1_000, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "icon_rock",
    )

  // ── Infinite: dots pulsing (stagger 200 ms each) ─────────────────────────
  val dot0Alpha by
    infiniteTransition.animateFloat(
      initialValue = 0.3f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(750, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "dot0",
    )
  val dot1Alpha by
    infiniteTransition.animateFloat(
      initialValue = 0.3f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(750, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
          initialStartOffset = StartOffset(200),
        ),
      label = "dot1",
    )
  val dot2Alpha by
    infiniteTransition.animateFloat(
      initialValue = 0.3f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(750, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
          initialStartOffset = StartOffset(400),
        ),
      label = "dot2",
    )

  // ── Entrance animations ───────────────────────────────────────────────────
  val logoAlpha = remember { Animatable(0f) }
  val logoScale = remember { Animatable(0.8f) }
  val titleAlpha = remember { Animatable(0f) }
  val titleOffsetY = remember { Animatable(20f) }
  val subtitleAlpha = remember { Animatable(0f) }
  val subtitleOffsetY = remember { Animatable(20f) }
  val dotsAlpha = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    // Logo at t=0ms (600ms duration)
    launch { logoAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    launch { logoScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    // Title at t=200ms
    delay(200)
    launch { titleAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
    launch { titleOffsetY.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
    // Subtitle at t=400ms
    delay(200)
    launch { subtitleAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
    launch { subtitleOffsetY.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
    // Dots at t=1 000ms
    delay(600)
    dotsAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
  }

  // ── Layout ────────────────────────────────────────────────────────────────
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors =
              listOf(
                PalabritaColors.SplashGradientStart,
                PalabritaColors.SplashGradientMid,
                PalabritaColors.SplashGradientEnd,
              )
          )
        ),
    contentAlignment = Alignment.Center,
  ) {
    // Radial centre glow (matches CSS radial-gradient white 10% → transparent 50%)
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.radialGradient(0f to Color.White.copy(alpha = 0.10f), 0.5f to Color.Transparent)
          )
    )

    // Centre column: icon + title + subtitle
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      // Frosted-glass icon container
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(96.dp)
            .graphicsLayer {
              scaleX = logoScale.value
              scaleY = logoScale.value
              alpha = logoAlpha.value
              rotationZ = iconRotation
            }
            .background(Color.White.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(24.dp)),
      ) {
        Icon(
          imageVector = Icons.Rounded.AutoAwesome,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(48.dp),
        )
      }

      Spacer(Modifier.height(24.dp))

      Text(
        text = stringResource(R.string.app_name),
        style =
          MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color.White,
          ),
        modifier =
          Modifier.graphicsLayer {
            alpha = titleAlpha.value
            translationY = with(density) { titleOffsetY.value.dp.toPx() }
          },
      )

      Spacer(Modifier.height(8.dp))

      Text(
        text = stringResource(R.string.splash_subtitle),
        style =
          MaterialTheme.typography.bodyLarge.copy(
            color = Color.White.copy(alpha = 0.90f),
            fontWeight = FontWeight.Medium,
          ),
        modifier =
          Modifier.graphicsLayer {
            alpha = subtitleAlpha.value
            translationY = with(density) { subtitleOffsetY.value.dp.toPx() }
          },
      )
    }

    // Bottom pulsing dots
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier =
        Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp).graphicsLayer {
          alpha = dotsAlpha.value
        },
    ) {
      listOf(dot0Alpha, dot1Alpha, dot2Alpha).forEach { dotAlpha ->
        Box(
          modifier =
            Modifier.size(8.dp)
              .graphicsLayer { alpha = dotAlpha }
              .background(Color.White, CircleShape)
        )
      }
    }
  }
}
