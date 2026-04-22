# Spec 05 — Game

## Summary

The game screen is the core of Palabrita. The player tries to discover the word of the day in up to 6 attempts, Wordle-style. Letters receive visual feedback by position. The player can reveal progressive hints. Difficulty adapts to the player's history.

## Game Mechanics

### Basic Rules

- The player has **6 attempts** to discover the word
- The word has between **5 and 8 letters** (dynamic per puzzle)
- Each attempt must be a complete word (with the same number of letters)
- After each attempt, each letter receives a color feedback:
  - 🟦 **Mint/Teal** (`#4ECDC4`): correct letter in the correct position
  - 🟧 **Amber/Gold** (`#FFB347`): letter exists in the word but is in the wrong position
  - 🟥 **Coral** (`#FF6B6B`): letter does not exist in the word
- The player can reveal **hints** (maximum 5, progressive from vaguest to most specific)
- **Win**: guessing the word in any attempt
- **Loss**: using all 6 attempts without guessing correctly

**Reward rules:**
- Win: earns XP (varies with difficulty, attempts, and hints used)
- Loss: no XP earned, nothing lost
- Streak: counts **consecutive days played**, regardless of win/loss. Played today = streak maintained.

### Letter Feedback Algorithm

For each attempt, the feedback is calculated as follows:

```
1. Mark all letters in the correct position as CORRECT (mint)
2. For each letter not marked as CORRECT:
   a. Count how many times that letter appears in the target word
   b. Subtract how many times it has already been marked as CORRECT
   c. If there are still remaining occurrences: PRESENT (amber)
   d. If not: ABSENT (coral)
3. Process left to right (for duplicate letters)
```

Example: word = "gatos", attempt = "gagas"
- g[0] → CORRECT (mint)
- a[1] → CORRECT (mint)
- g[2] → ABSENT (coral — 'g' only appears once and has already been used)
- a[3] → ABSENT (coral — 'a' only appears once and has already been used)
- s[4] → CORRECT (mint)

### Difficulty Selection (Free Play Mode only)

> **Note (Spec 11):** In Daily Challenges, difficulty is automatic (progressive). The DifficultyPicker only appears in Free Play Mode.

Before each game in Free Play Mode, the player chooses the difficulty:

```
┌──────────────────────────────┐
│ Choose difficulty              │
│                              │
│  ● ⭐      Easy         1 XP │
│  ○ ⭐⭐    Normal       2 XP │
│  ○ ⭐⭐⭐  Hard         3 XP │
│  🔒 ⭐⭐⭐⭐ Challenging  5 XP │
│  🔒 ⭐⭐⭐⭐⭐ Expert    8 XP │
│                              │
│  Recommended: ⭐⭐ Normal      │
│                              │
│  [Play]                      │
└──────────────────────────────┘
```

**Rules:**
- Available levels: up to `maxUnlockedDifficulty + 1` (can try 1 above what they have already unlocked)
- Locked levels (🔒): show "Unlock by winning at the previous level"
- "Recommended": always `currentDifficulty` (the level the automatic progression placed the player at)
- Automatic promotion continues working — unlocks levels, does not force the player
- Automatic demotion only changes the recommendation, does not lock already unlocked levels
- New player: only level 1 available (level 2 appears with 🔒 but clicking shows "Win 5 Easy games to unlock")
- The player can play below the recommended level (earns less XP, but accelerates streak bonus)
- Choosing above the recommended: more risk of losing, but more XP if winning

### Puzzle Selection

```
1. Player chooses difficulty (chosenDifficulty)
2. Calculate wordLength via difficultyToWordLength(chosenDifficulty, wordSizePreference)
3. Fetch unplayed puzzle: PuzzleDao.getNextUnplayed(language, chosenDifficulty)
4. If exists: use this puzzle
5. If not found (no pre-generated puzzles at this difficulty):
   a. AI Mode: generate inline via PuzzleGenerator (show loading)
   b. Light Mode: fetch from static dataset by nearest difficulty
6. Mark puzzle as "in game" (create GameSessionEntity)
```

### Attempt Validation

Before accepting an attempt:
- Must have exactly the same number of letters as the target word
- Must contain only `[a-z]` characters
- **NOT** necessary to validate if it is a real word (V1 — no local dictionary)

## UI — Compose

### General Layout

```
┌──────────────────────────────┐
│ ← │ CHALLENGE 2/3 │ 💡 3/5  │  ← Contextual header
├──────────────────────────────┤
│                              │
│     ┌─┬─┬─┬─┬─┬─┐          │
│     │ │ │ │ │ │ │  ← attempt 1 (filled)
│     ├─┼─┼─┼─┼─┼─┤          │
│     │ │ │ │ │ │ │  ← attempt 2 (filled)
│     ├─┼─┼─┼─┼─┼─┤          │
│     │ │ │ │ │ │ │  ← attempt 3 (active)
│     ├─┼─┼─┼─┼─┼─┤          │
│     │ │ │ │ │ │ │  ← empty
│     ├─┼─┼─┼─┼─┼─┤          │
│     │ │ │ │ │ │ │  ← empty
│     ├─┼─┼─┼─┼─┼─┤          │
│     │ │ │ │ │ │ │  ← empty
│     └─┴─┴─┴─┴─┴─┘          │
│                              │

│  ┌──────────────────────────┐│
│  │  Q W E R T Y U I O P    ││
│  │   A S D F G H J K L     ││
│  │  ⌫  Z X C V B N M  ↵   ││
│  └──────────────────────────┘│
└──────────────────────────────┘
```

### Components

**WordGrid**
- Rows: 6 (fixed — maximum attempts)
- Columns: dynamic (5-8, based on word length)
- Each cell: `Box` with centered `Text` + color background
- Flip animation when revealing feedback (after submitting attempt)
- Shake animation on row if attempt is invalid (wrong length)
- Bounce/pop animation on correct guess (all cells mint)

**GameKeyboard**
- QWERTY layout (adapted to locale if necessary — V1 uses standard QWERTY)
- Each key shows color state based on attempts:
  - Unused: neutral color
  - CORRECT in any attempt: mint (`#4ECDC4`)
  - PRESENT in any attempt: amber (`#FFB347`)
  - ABSENT in all attempts: coral (`#FF6B6B`)
- Special keys: Backspace (⌫) and Enter (↵)
- Keys with accessible touch size

**HintButton**
- Floating or fixed below the grid
- Shows: lightbulb icon + "Hint (X/5)"
- On click: reveals the next hint (card slide-in animation)
- Revealed hint stays visible until the end of the game
- If all 5 hints revealed: button disabled

**HintCard**
- Card with the revealed hint
- Numbering: "Hint 1:", "Hint 2:", etc.
- Position: between the grid and the keyboard (scrollable if necessary)
- Animation: fade-in + slide from bottom

### Contextual Header

The game header shows different context for dailies vs free play:

| Mode | Header | Example |
|------|--------|---------|
| Daily Challenge | "CHALLENGE N/3" | "CHALLENGE 2/3" |
| Free Play | "FREE" | "FREE" |

**Header elements:**
- **Left**: back button (←) with confirmation "Abandon game?"
- **Center**: game context ("CHALLENGE N/3" or "FREE")
- **Right**: hints counter (💡 3/5)

### Back Button — Confirmation

When pressing ← or system back during an active game:

```
┌──────────────────────────────┐
│                              │
│    Abandon game?             │
│                              │
│    Your progress in this     │
│    game will be lost.        │
│                              │
│    [Continue playing]        │  ← primary
│    [Abandon]                 │  ← destructive
│                              │
└──────────────────────────────┘
```

- "Continue playing" → closes dialog, returns to game
- "Abandon" → GameSession marked as abandoned, navigates to Home
- If the game is already over (WON/LOST): back navigates directly without confirmation

### Result Screen (Win and Loss)

> **Change (Spec 12):** The Chat Card is now the main CTA of the ResultScreen. See Spec 12 for details on Chat AI Engagement.

```
┌──────────────────────────────┐
│                              │
│     🎉 Congratulations!      │  ← or "😔 Not this time"
│     You discovered it in 3/6 │  ← or "The word was: GATOS"
│                              │
│     Word: GATOS              │
│     Category: Animal         │
│     +8 XP ✨                 │
│                              │
│  ┌────────────────────────┐  │
│  │  💬 Explore "GATOS"    │  │  ← MAIN CTA (Spec 12)
│  │                        │  │
│  │  🧬 Etymology          │  │
│  │  🌎 Fun fact           │  │
│  │  📝 Example sentences  │  │
│  │                        │  │
│  │  +1 XP bonus ✨        │  │
│  │                        │  │
│  │  [ EXPLORE NOW ]       │  │
│  └────────────────────────┘  │
│                              │
│  [▶ Play again]              │  ← secondary
│  [📤 Share]                  │  ← tertiary
│                              │
└──────────────────────────────┘
```

**Visual hierarchy:**
1. Result (congratulations/loss + word + XP earned)
2. **Chat Card** (main CTA — `primaryContainer` bg, `primary` border)
3. Play again (secondary button)
4. Share (ghost button)

**Light Mode:** Chat Card replaced by inline static curiosity (no navigation).

**"Play again":**
- In daily: navigates to the next daily (if available) or to Home
- In free play: returns to DifficultyPicker

```
│                              │
│    Next puzzle in XX:XX      │
│                              │
└──────────────────────────────┘
```

### Share Format

> **Change (Spec 11):** Puzzles are unique per player (local AI). There is no "puzzle of the day #123". The share highlights streak, tier, and XP — player identity, not puzzle identity.

**Win:**

```
Palabrita 🔥12 · Astuto · 350 XP

Challenge 1 ⭐ — 3/6
🟦🟦🟧⬜⬜
🟦🟦🟦⬜🟦
🟦🟦🟦🟦🟦

💡 1 hint used · +8 XP today
```

**Loss:**

```
Palabrita 🔥12 · Astuto · 350 XP

Challenge 2 ⭐⭐ — X/6
🟥🟧🟥🟥🟥🟥
🟥🟥🟦🟥🟥🟥
🟥🟦🟦🟥🟦🟥
🟥🟧🟦🟦🟥🟥
🟦🟦🟦🟥🟦🟥
🟥🟦🟦🟦🟦🟥

💡 3 hints used
```

**Free play:**

```
Palabrita 🔥12 · Astuto · 350 XP

Free ⭐⭐⭐ — 4/6
🟦🟧🟥🟥🟥🟥
🟦🟦🟧🟥🟥🟥
🟦🟦🟦🟥🟦🟥
🟦🟦🟦🟦🟦🟦

The word was: GATOS 🐱
💡 2 hints · +4 XP
```

**Details:**
- Header: streak + tier + total XP (player identity)
- Context: "Challenge N/3" or "Free" + difficulty stars
- Word shown in share (each player has a unique word, no spoiler)
- Category-themed emoji (optional, emoji-free fallback)
- XP earned in the footer

## Adaptive Difficulty

### XP and Progression System

The player accumulates XP by winning games. XP never decreases. The tier (ranking) is derived from total XP and never decreases — it is a reward, not a punishment. **Difficulty**, on the other hand, can go up and down based on performance.

### XP Calculation per Game

```kotlin
fun calculateXpForGame(
    won: Boolean,
    attempts: Int,
    difficulty: Int,
    currentStreak: Int,
    hintsUsed: Int
): Int {
    if (!won) return 0

    val baseXp = when (difficulty) {
        1 -> 1
        2 -> 2
        3 -> 3
        4 -> 5
        5 -> 8
        else -> 1
    }

    val attemptBonus = when (attempts) {
        1 -> 3
        2 -> 1
        else -> 0
    }

    val streakBonus = when {
        currentStreak >= 30 && currentStreak % 30 == 0 -> 20
        currentStreak >= 7 && currentStreak % 7 == 0 -> 5
        else -> 0
    }

    // Hint penalty: -1 XP per hint used (cannot zero out the base)
    val hintPenalty = hintsUsed

    return (baseXp + attemptBonus + streakBonus - hintPenalty).coerceAtLeast(1)
}
```

**Hint penalty:**
- Each hint used reduces **1 XP** from the total
- The minimum XP per win is always **1** (never zero — the player won, they deserve something)
- Hints do not affect the streak bonus (streak bonus is separate)

Example: win at level 3 (base 3 XP) + 1st attempt (+3) + 2 hints used (-2) = **4 XP**
Example: win at level 1 (base 1 XP) + 5 hints used (-5) = **1 XP** (minimum)

### Tier (Ranking) — derived from XP

```kotlin
enum class PlayerTier(val minXp: Int, val displayName: String) {
    NOVATO(0, "Novato"),
    CURIOSO(50, "Curioso"),
    ASTUTO(150, "Astuto"),
    SABIO(400, "Sábio"),
    EPICO(1000, "Épico"),
    LENDARIO(2500, "Lendário");

    companion object {
        fun fromXp(totalXp: Int): PlayerTier =
            entries.last { totalXp >= it.minXp }
    }
}
```

Tier **never decreases**. If the player stops playing for months and returns, they keep their tier.

### Difficulty Promotion / Demotion

The difficulty of the next puzzle is automatically adjusted:

```kotlin
fun checkDifficultyProgression(stats: PlayerStats): Int {
    val current = stats.currentDifficulty
    val winsAtCurrent = stats.gamesWonByDifficulty[current] ?: 0
    val winRateAtCurrent = stats.winRateByDifficulty[current] ?: 0f
    val requiredWins = if (stats.currentStreak >= 7) 4 else 5

    // Promotion: won N+ games at current level with winRate ≥ 70%
    if (winsAtCurrent >= requiredWins && winRateAtCurrent >= 0.70f && current < 5) {
        return current + 1
    }

    // Demotion: lost 3 in a row at current level
    if (stats.consecutiveLossesAtCurrent >= 3 && current > 1) {
        return current - 1
    }

    return current  // no change
}
```

**Rules:**
- Promotion requires **5 wins** at the current level with **winRate ≥ 70%** (4 wins if streak ≥ 7 days)
- Demotion after **3 consecutive losses** at the current level
- New player starts at **level 1**
- On promotion/demotion: `consecutiveLossesAtCurrent` resets to 0
- On promotion: `gamesWonByDifficulty[novoNível]` starts counting from 0 already stored (history preserved)

### How Difficulty Affects the Puzzle

| Difficulty | Size | Word Type | Hint Style |
|---|---|---|---|
| 1 (easy) | 5 letters | Very common, everyday | Direct hints |
| 2 | 5-6 letters | Common | Clear hints |
| 3 (medium) | 6-7 letters | Less frequent | Moderate hints |
| 4 | 7-8 letters | Uncommon | Vaguer hints |
| 5 (hard) | 7-8 letters | Rare, technical | Abstract hints |

```kotlin
fun difficultyToWordLength(difficulty: Int, wordSizePreference: String): IntRange {
    // If the player chose a fixed size in settings (Astuto+ tier)
    return when (wordSizePreference) {
        "SHORT" -> 5..6
        "LONG" -> 7..9
        "EPIC" -> 8..10
        else -> when (difficulty) {  // "DEFAULT" — dynamic by difficulty
            1 -> 5..5
            2 -> 5..6
            3 -> 6..7
            4 -> 7..8
            5 -> 7..8
            else -> 5..6
        }
    }
}
```

When the player uses a fixed range, difficulty still affects the **word rarity** and **hint style** (controlled by the prompt), but not the length.

Difficulty 4 and 5 have the same letter range, but differ in **word rarity** (controlled by the prompt: "uncommon" vs "rare/technical").

These parameters are passed in the LLM prompt as `min_length`, `max_length`, `target_difficulty`.

## GameViewModel

### State

```kotlin
data class GameState(
    val puzzle: Puzzle?,
    val chosenDifficulty: Int,
    val availableDifficulties: List<DifficultyOption>,
    val attempts: List<Attempt>,
    val currentInput: String,
    val revealedHints: List<String>,
    val keyboardState: Map<Char, LetterState>,
    val gameStatus: GameStatus,
    val isLoading: Boolean,
    val error: String?,
    // New fields (Spec 10, 11):
    val gameContext: GameContext = GameContext.FreePlay,
    val showAbandonDialog: Boolean = false,
)

sealed class GameContext {
    data class DailyChallenge(val index: Int, val total: Int = 3) : GameContext()
    data object FreePlay : GameContext()
}

data class DifficultyOption(
    val level: Int,
    val label: String,        // "Easy", "Normal", etc.
    val baseXp: Int,
    val isUnlocked: Boolean,
    val isRecommended: Boolean
)

data class Attempt(
    val word: String,
    val feedback: List<LetterFeedback>
)

data class LetterFeedback(
    val letter: Char,
    val state: LetterState
)

enum class LetterState { CORRECT, PRESENT, ABSENT, UNUSED }
enum class GameStatus { CHOOSING_DIFFICULTY, PLAYING, WON, LOST, LOADING }
```

### Actions

```kotlin
sealed class GameAction {
    data class SelectDifficulty(val level: Int) : GameAction()
    data object StartGame : GameAction()
    data class TypeLetter(val letter: Char) : GameAction()
    data object DeleteLetter : GameAction()
    data object SubmitAttempt : GameAction()
    data object RevealHint : GameAction()
    data object ShareResult : GameAction()
    data object NavigateToChat : GameAction()
    data object NavigateToStats : GameAction()
    data object LoadNextPuzzle : GameAction()
    data object BackPressed : GameAction()         // New: confirm abandon
    data object ConfirmAbandon : GameAction()       // New: confirm dialog
    data object DismissAbandonDialog : GameAction() // New: cancel dialog
}
```

## PuzzleGenerationWorker (WorkManager)

- **Trigger**: daily periodic (minimum 15 min WorkManager interval, but set to 24h)
- **Constraint**: device idle or charging (to not impact UX)
- **Logic**:
  1. Check `PuzzleDao.countUnplayed(language)`
  2. If < 3: generate 7 new puzzles via `PuzzleGenerator`
  3. If Light mode: do nothing (static dataset is finite)
- **Retry**: if it fails, WorkManager retries with exponential backoff

## Edge Cases

| Scenario | Behavior |
|---|---|
| User types fewer letters and presses Enter | Shake animation on row + ignore |
| Duplicate word (same attempt twice) | Allow (V1 — no restriction) |
| All puzzles played (AI mode) | Show loading, generate inline via PuzzleGenerator |
| All puzzles played (Light mode) | Message "Wait for app update" |
| App closed during game | Save state to GameSessionEntity, restore on reopen |
| Screen rotation | Compose handles automatically (ViewModel preserves state) |
| Physical keyboard | Capture key events and map to GameAction |

## Acceptance Criteria

- [ ] Grid renders correctly for words of 5, 6, 7, and 8 letters
- [ ] Color feedback is correct for duplicate letters (per algorithm)
- [ ] Keyboard updates colors correctly after each attempt
- [ ] Flip animation works when revealing feedback
- [ ] Shake animation works for invalid attempt
- [ ] Hints reveal progressively (1 to 5)
- [ ] Win correctly detected and navigates to result screen
- [ ] Loss correctly detected after 6 attempts
- [ ] Share generates correct emoji grid
- [ ] State persists between app kills (via GameSessionEntity)
- [ ] Adaptive difficulty changes based on history
- [ ] Difficulty selector shows unlocked levels correctly
- [ ] Locked levels are not selectable
- [ ] Recommended level is highlighted
- [ ] "Explore the word" only appears in AI mode
- [ ] WorkManager generates puzzles in background when stock < 3
- [ ] Header shows "CHALLENGE N/3" for dailies and "FREE" for free play
- [ ] Back button during active game shows "Abandon game?"
- [ ] "Continue playing" closes dialog and returns to game
- [ ] "Abandon" marks GameSession as abandoned and navigates to Home
- [ ] Back without confirmation if game is already over (WON/LOST)
- [ ] Chat Card is the main CTA in ResultScreen (above "play again")
- [ ] Share shows streak + tier + XP in the header
- [ ] Share shows context ("Challenge N/3" or "Free")
- [ ] DifficultyPicker only appears in Free Play Mode (not in dailies)
