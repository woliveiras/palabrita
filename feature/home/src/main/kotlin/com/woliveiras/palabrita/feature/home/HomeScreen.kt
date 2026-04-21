package com.woliveiras.palabrita.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.LocalGameColors
import com.woliveiras.palabrita.core.common.R as CommonR

@Composable
fun HomeScreen(
  onNavigateToGame: (dailyChallengeIndex: Int?, difficulty: Int?) -> Unit,
  onNavigateToFreePlay: () -> Unit,
  onNavigateToChat: (Long) -> Unit,
  onNavigateToStats: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  if (state.isLoading) {
    LoadingHome()
    return
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Title
    Text(
      text = stringResource(CommonR.string.home_title),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(vertical = 8.dp),
    )

    Spacer(Modifier.height(16.dp))

    // Streak Card
    StreakCard(
      streak = state.streak,
      nextMilestone = state.nextStreakMilestone,
    )

    Spacer(Modifier.height(16.dp))

    // Daily Challenges Card
    DailyChallengesCard(
      challenges = state.dailyChallenges,
      completedCount = state.completedDailies,
      allComplete = state.allDailiesComplete,
      onStartChallenge = { index ->
        val challenge = state.dailyChallenges.getOrNull(index)
        if (challenge != null && challenge.state == DailyChallengeState.AVAILABLE) {
          onNavigateToGame(index, challenge.difficulty)
        }
      },
      onExploreChat = { puzzleId -> onNavigateToChat(puzzleId) },
    )

    Spacer(Modifier.height(16.dp))

    // Quick Stats
    QuickStatsRow(
      totalPlayed = state.totalPlayed,
      winRate = state.winRate,
      playerTier = state.playerTier,
      totalXp = state.totalXp,
      onClick = onNavigateToStats,
    )

    // Generation Indicator
    if (state.isGeneratingPuzzles || state.generationComplete) {
      Spacer(Modifier.height(16.dp))
      GenerationIndicator(
        isGenerating = state.isGeneratingPuzzles,
        isComplete = state.generationComplete,
      )
    }

    Spacer(Modifier.height(24.dp))
  }
}

@Composable
private fun LoadingHome() {
  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CircularProgressIndicator()
  }
}

// --- Streak Card ---

@Composable
private fun StreakCard(streak: Int, nextMilestone: Int) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer,
    ),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = if (streak > 0) stringResource(CommonR.string.home_streak_active, streak) else stringResource(CommonR.string.home_streak_start),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(8.dp))
      LinearProgressIndicator(
        progress = { (streak.toFloat() / nextMilestone).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth().height(8.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = stringResource(CommonR.string.home_streak_milestone, nextMilestone),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
      )
    }
  }
}

// --- Daily Challenges Card ---

@Composable
private fun DailyChallengesCard(
  challenges: List<DailyChallenge>,
  completedCount: Int,
  allComplete: Boolean,
  onStartChallenge: (Int) -> Unit,
  onExploreChat: (Long) -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(CommonR.string.home_daily_title, completedCount),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(12.dp))

      challenges.forEach { challenge ->
        DailyChallengeRow(
          challenge = challenge,
          onStart = { onStartChallenge(challenge.index) },
          onExploreChat = { challenge.puzzleId?.let(onExploreChat) },
        )
        Spacer(Modifier.height(8.dp))
      }

      if (allComplete) {
        Spacer(Modifier.height(4.dp))
        Text(
          text = stringResource(CommonR.string.home_daily_complete),
          style = MaterialTheme.typography.labelLarge,
          color = LocalGameColors.current.correct,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
        )
      } else {
        val nextAvailable = challenges.firstOrNull { it.state == DailyChallengeState.AVAILABLE }
        nextAvailable?.let {
          Button(
            onClick = { onStartChallenge(it.index) },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(stringResource(CommonR.string.home_daily_play, it.index + 1))
          }
        }
      }
    }
  }
}

@Composable
private fun DailyChallengeRow(
  challenge: DailyChallenge,
  onStart: () -> Unit,
  onExploreChat: () -> Unit,
) {
  val stateIcon = when (challenge.state) {
    DailyChallengeState.COMPLETED -> Icons.Rounded.CheckCircle
    DailyChallengeState.AVAILABLE -> Icons.Rounded.PlayArrow
    DailyChallengeState.LOCKED -> Icons.Rounded.Lock
  }
  val stateColor = when (challenge.state) {
    DailyChallengeState.COMPLETED -> LocalGameColors.current.correct
    DailyChallengeState.AVAILABLE -> MaterialTheme.colorScheme.primary
    DailyChallengeState.LOCKED -> MaterialTheme.colorScheme.onSurfaceVariant
  }
  val textAlpha = if (challenge.state == DailyChallengeState.LOCKED) 0.5f else 1f

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Number
    Text(
      text = "${challenge.index + 1}",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = stateColor,
      modifier = Modifier.width(24.dp),
    )

    // State icon
    Icon(
      stateIcon,
      contentDescription = null,
      tint = stateColor,
      modifier = Modifier.size(20.dp),
    )
    Spacer(Modifier.width(8.dp))

    // Difficulty stars
    Row(modifier = Modifier.weight(1f)) {
      repeat(challenge.difficulty) {
        Icon(
          Icons.Rounded.Star,
          contentDescription = null,
          modifier = Modifier.size(14.dp),
          tint = stateColor.copy(alpha = textAlpha),
        )
      }
    }

    // Result or status text
    when {
      challenge.result != null -> {
        Text(
          text = "${challenge.result.attempts}/6",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (challenge.state == DailyChallengeState.COMPLETED && challenge.puzzleId != null) {
          Spacer(Modifier.width(8.dp))
          IconButton(
            onClick = onExploreChat,
            modifier = Modifier.size(28.dp),
          ) {
            Icon(
              Icons.Rounded.ChatBubble,
              contentDescription = stringResource(CommonR.string.chat_card_cta),
              modifier = Modifier.size(18.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
      challenge.state == DailyChallengeState.LOCKED -> {
        Text(
          text = stringResource(CommonR.string.home_daily_locked),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
      }
      else -> {}
    }
  }
}

// --- Free Play Card ---

@Composable
private fun FreePlayCard(onStart: () -> Unit) {
  OutlinedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Rounded.Casino,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "MODO LIVRE",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
      Spacer(Modifier.height(4.dp))
      Text(
        text = "Escolha dificuldade e jogue quantas vezes quiser",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(12.dp))
      OutlinedButton(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("JOGAR")
      }
    }
  }
}

// --- Quick Stats ---

@Composable
private fun QuickStatsRow(
  totalPlayed: Int,
  winRate: Float,
  playerTier: String,
  totalXp: Int,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
      StatItem(value = "$totalPlayed", label = stringResource(CommonR.string.home_stats_games))
      StatItem(value = "${(winRate * 100).toInt()}%", label = stringResource(CommonR.string.home_stats_wins))
      StatItem(value = playerTier, label = stringResource(CommonR.string.home_stats_tier))
      StatItem(value = "$totalXp", label = stringResource(CommonR.string.home_stats_xp))
    }
  }
}

@Composable
private fun StatItem(value: String, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

// --- Generation Indicator ---

@Composable
private fun GenerationIndicator(isGenerating: Boolean, isComplete: Boolean) {
  OutlinedCard(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(12.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (isGenerating) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text(
          text = stringResource(CommonR.string.home_generating),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      } else if (isComplete) {
        Text(
          text = stringResource(CommonR.string.home_generation_complete),
          style = MaterialTheme.typography.bodyMedium,
          color = LocalGameColors.current.correct,
        )
      }
    }
  }
}
