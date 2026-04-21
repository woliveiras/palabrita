package com.woliveiras.palabrita.feature.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woliveiras.palabrita.core.common.LocalGameColors
import androidx.compose.ui.res.stringResource
import com.woliveiras.palabrita.core.common.R as CommonR

@Composable
fun GameScreen(
  onNavigateToChat: (Long) -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: GameViewModel = hiltViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(Unit) {
    viewModel.loadDifficultyOptions()
  }

  AnimatedContent(
    targetState = state.gameStatus,
    transitionSpec = { fadeIn() togetherWith fadeOut() },
    label = "game-status",
  ) { status ->
    when (status) {
      GameStatus.CHOOSING_DIFFICULTY -> DifficultyPickerScreen(
        options = state.availableDifficulties,
        chosen = state.chosenDifficulty,
        onSelect = { viewModel.onAction(GameAction.SelectDifficulty(it)) },
        onStart = { viewModel.onAction(GameAction.StartGame) },
        onSettings = onNavigateToSettings,
      )
      GameStatus.LOADING -> LoadingScreen()
      GameStatus.PLAYING -> PlayingScreen(
        state = state,
        onTypeLetter = { viewModel.onAction(GameAction.TypeLetter(it)) },
        onDelete = { viewModel.onAction(GameAction.DeleteLetter) },
        onSubmit = { viewModel.onAction(GameAction.SubmitAttempt) },
        onRevealHint = { viewModel.onAction(GameAction.RevealHint) },
      )
      GameStatus.WON -> ResultScreen(
        won = true,
        puzzle = state.puzzle,
        attempts = state.attempts,
        hintsUsed = state.revealedHints.size,
        onExplore = { state.puzzle?.let { onNavigateToChat(it.id) } },
        onShare = { viewModel.onAction(GameAction.ShareResult) },
        onPlayAgain = { viewModel.onAction(GameAction.LoadNextPuzzle) },
      )
      GameStatus.LOST -> ResultScreen(
        won = false,
        puzzle = state.puzzle,
        attempts = state.attempts,
        hintsUsed = state.revealedHints.size,
        onExplore = { state.puzzle?.let { onNavigateToChat(it.id) } },
        onShare = { viewModel.onAction(GameAction.ShareResult) },
        onPlayAgain = { viewModel.onAction(GameAction.LoadNextPuzzle) },
      )
    }
  }
}

// --- Difficulty Picker ---

@Composable
private fun DifficultyPickerScreen(
  options: List<DifficultyOption>,
  chosen: Int,
  onSelect: (Int) -> Unit,
  onStart: () -> Unit,
  onSettings: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
    ) {
      androidx.compose.material3.IconButton(onClick = onSettings) {
        Icon(
          Icons.Rounded.Settings,
          contentDescription = stringResource(CommonR.string.settings),
        )
      }
    }
    Spacer(Modifier.height(16.dp))
    Text(
      text = stringResource(CommonR.string.difficulty_title),
      style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(Modifier.height(24.dp))

    options.forEach { option ->
      DifficultyCard(
        option = option,
        isChosen = option.level == chosen,
        onClick = { onSelect(option.level) },
      )
      Spacer(Modifier.height(8.dp))
    }

    Spacer(Modifier.weight(1f))
    Button(
      onClick = onStart,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(CommonR.string.play))
    }
  }
}

@Composable
private fun DifficultyCard(
  option: DifficultyOption,
  isChosen: Boolean,
  onClick: () -> Unit,
) {
  val border = if (isChosen) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
  val colors = if (isChosen)
    CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
  else CardDefaults.outlinedCardColors()

  OutlinedCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    border = border ?: CardDefaults.outlinedCardBorder(),
    colors = colors,
    enabled = option.isSelectable,
  ) {
    Row(
      modifier = Modifier.padding(16.dp).fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (!option.isSelectable) {
        Icon(
          Icons.Rounded.Lock,
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
      }
      Row(modifier = Modifier.weight(1f)) {
        repeat(option.level) {
          Icon(
            Icons.Rounded.Star,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (option.isSelectable) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Spacer(Modifier.width(8.dp))
        Text(
          text = stringResource(option.labelRes),
          style = MaterialTheme.typography.titleSmall,
        )
      }
      Text(
        text = "${option.baseXp} XP",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (option.isRecommended) {
        Spacer(Modifier.width(8.dp))
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = MaterialTheme.colorScheme.tertiary,
        ) {
          Text(
            text = stringResource(CommonR.string.recommended),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiary,
          )
        }
      }
    }
  }
}

// --- Loading ---

@Composable
private fun LoadingScreen() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      CircularProgressIndicator()
      Spacer(Modifier.height(16.dp))
      Text(stringResource(CommonR.string.loading_puzzle), style = MaterialTheme.typography.bodyLarge)
    }
  }
}

// --- Playing ---

@Composable
private fun PlayingScreen(
  state: GameState,
  onTypeLetter: (Char) -> Unit,
  onDelete: () -> Unit,
  onSubmit: () -> Unit,
  onRevealHint: () -> Unit,
) {
  val puzzle = state.puzzle ?: return
  val wordLength = puzzle.word.length
  var showHintsDialog by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(horizontal = 8.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Header
    Text(
      text = "Palabrita",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(8.dp))

    // Word Grid — fluid width, capped at 400dp
    WordGrid(
      attempts = state.attempts,
      currentInput = state.currentInput,
      wordLength = wordLength,
      maxAttempts = 6,
      modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
    )

    Spacer(Modifier.height(8.dp))

    // Hint button
    val hintsRemaining = puzzle.hints.size - state.revealedHints.size
    FilledTonalButton(
      onClick = {
        if (hintsRemaining > 0) onRevealHint()
        showHintsDialog = true
      },
      enabled = hintsRemaining > 0 || state.revealedHints.isNotEmpty(),
    ) {
      Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(4.dp))
      Text(
        if (state.revealedHints.isNotEmpty() && hintsRemaining == 0)
          stringResource(CommonR.string.hint_view_all)
        else
          stringResource(CommonR.string.hint_button, hintsRemaining, puzzle.hints.size)
      )
    }

    // Push keyboard to bottom
    Spacer(Modifier.weight(1f))

    // Keyboard
    GameKeyboard(
      keyboardState = state.keyboardState,
      onKey = onTypeLetter,
      onDelete = onDelete,
      onSubmit = onSubmit,
    )
  }

  // Hints dialog
  if (showHintsDialog && state.revealedHints.isNotEmpty()) {
    HintsDialog(
      hints = state.revealedHints,
      onDismiss = { showHintsDialog = false },
    )
  }
}

// --- Word Grid ---

@Composable
private fun WordGrid(
  attempts: List<Attempt>,
  currentInput: String,
  wordLength: Int,
  maxAttempts: Int,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    for (row in 0 until maxAttempts) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
      ) {
        when {
          row < attempts.size -> {
            // Submitted attempt
            val attempt = attempts[row]
            attempt.feedback.forEach { fb ->
              LetterCell(letter = fb.letter, state = fb.state)
            }
          }
          row == attempts.size -> {
            // Current input row
            for (col in 0 until wordLength) {
              val letter = currentInput.getOrNull(col)
              LetterCell(letter = letter, state = LetterState.UNUSED)
            }
          }
          else -> {
            // Empty row
            repeat(wordLength) {
              LetterCell(letter = null, state = LetterState.UNUSED)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LetterCell(letter: Char?, state: LetterState) {
  val gameColors = LocalGameColors.current
  val bgColor = when (state) {
    LetterState.CORRECT -> gameColors.correct
    LetterState.PRESENT -> gameColors.present
    LetterState.ABSENT -> gameColors.absent
    LetterState.UNUSED -> MaterialTheme.colorScheme.surfaceVariant
  }
  val textColor = if (state == LetterState.UNUSED)
    MaterialTheme.colorScheme.onSurface
  else gameColors.onFeedback

  Box(
    modifier = Modifier
      .size(62.dp)
      .background(bgColor, RoundedCornerShape(6.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = letter?.uppercaseChar()?.toString() ?: "",
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      color = textColor,
    )
  }
}

// --- Hints Dialog ---

@Composable
private fun HintsDialog(
  hints: List<String>,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(CommonR.string.hints_dialog_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        hints.forEachIndexed { index, hint ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
          ) {
            Text(
              text = stringResource(CommonR.string.hint_label, index + 1, hint),
              modifier = Modifier.padding(12.dp),
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(CommonR.string.close))
      }
    },
  )
}

// --- Keyboard ---

private val ROW1 = "qwertyuiop".toList()
private val ROW2 = "asdfghjkl".toList()
private val ROW3 = "zxcvbnm".toList()

@Composable
private fun GameKeyboard(
  keyboardState: Map<Char, LetterState>,
  onKey: (Char) -> Unit,
  onDelete: () -> Unit,
  onSubmit: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    KeyRow(ROW1, keyboardState, onKey)
    KeyRow(ROW2, keyboardState, onKey)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      // Backspace
      Surface(
        onClick = onDelete,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(56.dp).weight(1.3f),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.Backspace, contentDescription = stringResource(CommonR.string.delete_action), modifier = Modifier.size(24.dp))
        }
      }
      ROW3.forEach { letter ->
        KeyButton(letter = letter, state = keyboardState[letter], onClick = { onKey(letter) }, modifier = Modifier.weight(1f).height(56.dp))
      }
      // Enter
      Surface(
        onClick = onSubmit,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.height(56.dp).weight(1.3f),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            Icons.Rounded.Send,
            contentDescription = stringResource(CommonR.string.send),
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
          )
        }
      }
    }
  }
}

@Composable
private fun KeyRow(
  letters: List<Char>,
  keyboardState: Map<Char, LetterState>,
  onKey: (Char) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    letters.forEach { letter ->
      KeyButton(letter = letter, state = keyboardState[letter], onClick = { onKey(letter) }, modifier = Modifier.weight(1f).height(56.dp))
    }
  }
}

@Composable
private fun KeyButton(letter: Char, state: LetterState?, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val gameColors = LocalGameColors.current
  val bgColor = when (state) {
    LetterState.CORRECT -> gameColors.correct
    LetterState.PRESENT -> gameColors.present
    LetterState.ABSENT -> gameColors.absent
    null, LetterState.UNUSED -> MaterialTheme.colorScheme.surfaceVariant
  }
  val textColor = if (state != null && state != LetterState.UNUSED)
    gameColors.onFeedback
  else MaterialTheme.colorScheme.onSurface

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(4.dp),
    color = bgColor,
    modifier = modifier,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = letter.uppercaseChar().toString(),
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
      )
    }
  }
}

// --- Result Screen ---

@Composable
private fun ResultScreen(
  won: Boolean,
  puzzle: com.woliveiras.palabrita.core.model.Puzzle?,
  attempts: List<Attempt>,
  hintsUsed: Int,
  onExplore: () -> Unit,
  onShare: () -> Unit,
  onPlayAgain: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.statusBars)
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = if (won) stringResource(CommonR.string.result_won) else stringResource(CommonR.string.result_lost),
      style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(Modifier.height(16.dp))

    if (won) {
      Text(
        text = stringResource(CommonR.string.result_attempts, attempts.size),
        style = MaterialTheme.typography.bodyLarge,
      )
    }

    Spacer(Modifier.height(12.dp))
    puzzle?.let {
      Text(
        text = stringResource(CommonR.string.result_word, it.wordDisplay),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = stringResource(CommonR.string.result_category, it.category),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (hintsUsed > 0) {
      Spacer(Modifier.height(8.dp))
      Text(
        text = stringResource(CommonR.string.result_hints_used, hintsUsed),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(Modifier.height(32.dp))

    if (puzzle?.source == com.woliveiras.palabrita.core.model.PuzzleSource.AI) {
      OutlinedButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(CommonR.string.result_explore))
      }
      Spacer(Modifier.height(8.dp))
    }

    Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(CommonR.string.share))
    }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(CommonR.string.play_again))
    }
  }
}
