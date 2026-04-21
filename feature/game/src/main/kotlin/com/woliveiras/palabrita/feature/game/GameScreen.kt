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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val MintColor = Color(0xFF4ECDC4)
private val AmberColor = Color(0xFFFFB347)
private val CoralColor = Color(0xFFFF6B6B)

@Composable
fun GameScreen(
  onNavigateToChat: (Long) -> Unit,
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
      )
      GameStatus.LOST -> ResultScreen(
        won = false,
        puzzle = state.puzzle,
        attempts = state.attempts,
        hintsUsed = state.revealedHints.size,
        onExplore = { state.puzzle?.let { onNavigateToChat(it.id) } },
        onShare = { viewModel.onAction(GameAction.ShareResult) },
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
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(Modifier.height(48.dp))
    Text(
      text = "Escolha a dificuldade",
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
      Text("Jogar")
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
          text = option.label,
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
            text = "Recomendado",
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
      Text("Preparando puzzle...", style = MaterialTheme.typography.bodyLarge)
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

  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Header
    Text(
      text = "Palabrita",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
    )
    Spacer(Modifier.height(16.dp))

    // Word Grid
    WordGrid(
      attempts = state.attempts,
      currentInput = state.currentInput,
      wordLength = wordLength,
      maxAttempts = 6,
    )

    Spacer(Modifier.height(12.dp))

    // Hints
    if (state.revealedHints.isNotEmpty()) {
      HintsList(hints = state.revealedHints)
      Spacer(Modifier.height(8.dp))
    }

    // Hint button
    val hintsRemaining = puzzle.hints.size - state.revealedHints.size
    FilledTonalButton(
      onClick = onRevealHint,
      enabled = hintsRemaining > 0,
    ) {
      Icon(Icons.Rounded.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(4.dp))
      Text("Dica ($hintsRemaining/${puzzle.hints.size})")
    }

    Spacer(Modifier.weight(1f))

    // Keyboard
    GameKeyboard(
      keyboardState = state.keyboardState,
      onKey = onTypeLetter,
      onDelete = onDelete,
      onSubmit = onSubmit,
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
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    for (row in 0 until maxAttempts) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
  val bgColor = when (state) {
    LetterState.CORRECT -> MintColor
    LetterState.PRESENT -> AmberColor
    LetterState.ABSENT -> CoralColor
    LetterState.UNUSED -> MaterialTheme.colorScheme.surfaceVariant
  }
  val textColor = if (state == LetterState.UNUSED)
    MaterialTheme.colorScheme.onSurface
  else Color.White

  Box(
    modifier = Modifier
      .size(44.dp)
      .background(bgColor, RoundedCornerShape(4.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = letter?.uppercaseChar()?.toString() ?: "",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = textColor,
    )
  }
}

// --- Hints List ---

@Composable
private fun HintsList(hints: List<String>) {
  LazyColumn(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    items(hints.withIndex().toList()) { (index, hint) ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
      ) {
        Text(
          text = "Dica ${index + 1}: $hint",
          modifier = Modifier.padding(12.dp),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
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
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    KeyRow(ROW1, keyboardState, onKey)
    KeyRow(ROW2, keyboardState, onKey)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      // Backspace
      Surface(
        onClick = onDelete,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(48.dp).width(48.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(Icons.Rounded.Backspace, contentDescription = "Apagar", modifier = Modifier.size(20.dp))
        }
      }
      ROW3.forEach { letter ->
        KeyButton(letter = letter, state = keyboardState[letter], onClick = { onKey(letter) })
      }
      // Enter
      Surface(
        onClick = onSubmit,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.height(48.dp).width(48.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            Icons.Rounded.Send,
            contentDescription = "Enviar",
            modifier = Modifier.size(20.dp),
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
  Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    letters.forEach { letter ->
      KeyButton(letter = letter, state = keyboardState[letter], onClick = { onKey(letter) })
    }
  }
}

@Composable
private fun KeyButton(letter: Char, state: LetterState?, onClick: () -> Unit) {
  val bgColor = when (state) {
    LetterState.CORRECT -> MintColor
    LetterState.PRESENT -> AmberColor
    LetterState.ABSENT -> CoralColor
    null, LetterState.UNUSED -> MaterialTheme.colorScheme.surfaceVariant
  }
  val textColor = if (state != null && state != LetterState.UNUSED)
    Color.White
  else MaterialTheme.colorScheme.onSurface

  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(4.dp),
    color = bgColor,
    modifier = Modifier.size(width = 32.dp, height = 48.dp),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = letter.uppercaseChar().toString(),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
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
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      text = if (won) "Parabéns!" else "Não foi dessa vez",
      style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(Modifier.height(16.dp))

    if (won) {
      Text(
        text = "Você descobriu em ${attempts.size}/6",
        style = MaterialTheme.typography.bodyLarge,
      )
    }

    Spacer(Modifier.height(12.dp))
    puzzle?.let {
      Text(
        text = "Palavra: ${it.wordDisplay}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = "Categoria: ${it.category}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (hintsUsed > 0) {
      Spacer(Modifier.height(8.dp))
      Text(
        text = "$hintsUsed dicas usadas",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    Spacer(Modifier.height(32.dp))

    if (puzzle?.source == com.woliveiras.palabrita.core.model.PuzzleSource.AI) {
      OutlinedButton(onClick = onExplore, modifier = Modifier.fillMaxWidth()) {
        Text("Explorar a palavra")
      }
      Spacer(Modifier.height(8.dp))
    }

    Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
      Text("Compartilhar")
    }
  }
}
