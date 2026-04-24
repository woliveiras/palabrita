# Spec 05 — Game

## Summary

The game screen is the core of Palabrita. The player tries to discover the word in up to 6 attempts, Wordle-style. Letters receive visual feedback by position. The player can reveal progressive hints. Difficulty is implicit via word length (4-8 letters), progressing through generation cycles.

## Game Mechanics

### Basic Rules

- The player has **6 attempts** to discover the word
- The word has between **4 and 8 letters** (dynamic per puzzle, determined by generation cycle)
- Each attempt must be a complete word (with the same number of letters)
- After each attempt, each letter receives a color feedback:
  - 🟦 **Mint/Teal** (`#4ECDC4`): correct letter in the correct position
  - 🟧 **Amber/Gold** (`#FFB347`): letter exists in the word but is in the wrong position
  - 🟥 **Coral** (`#FF6B6B`): letter does not exist in the word
- The player can reveal **hints** (maximum 3, progressive from vaguest to most specific)
- **Win**: guessing the word in any attempt
- **Loss**: using all 6 attempts without guessing correctly

**Reward rules:**
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

### Puzzle Selection

```
1. Fetch next unplayed puzzle: PuzzleDao.getNextUnplayed(language) — ordered by word length within the batch
2. If exists: use this puzzle
3. If not found (no pre-generated puzzles):
   a. AI Mode: prompt user to generate more (triggers new generation cycle)
   b. Light Mode: fetch from static dataset
4. Mark puzzle as "in game" (create GameSessionEntity)
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
- Columns: dynamic (4-8, based on word length)
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
- **Accent keyboard toggle** (Spec 17): a 4th row appears for PT/ES languages with accented characters (á, ã, ç, ñ, etc.). Toggle button "ÁÀ" shows/hides the row. Pressing an accent key types the **base letter** into the input (game logic is ASCII). Accent key colors inherit from the base letter's keyboard state. Hidden for English.

**HintButton**
- Floating or fixed below the grid
- Shows: lightbulb icon + "Hint (X/N)" where N is the total hints available for this puzzle (3)
- On click: reveals the next hint (card slide-in animation)
- Revealed hint stays visible until the end of the game
- If all hints revealed: button disabled

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
│                              │
│  ┌────────────────────────┐  │
│  │  💬 Explore "GATOS"    │  │  ← MAIN CTA (Spec 12)
│  │                        │  │
│  │  🧬 Etymology          │  │
│  │  🌎 Fun fact           │  │
│  │  📝 Example sentences  │  │
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
1. Result (congratulations/loss + word)
2. **Chat Card** (main CTA — `primaryContainer` bg, `primary` border)
3. Play again (secondary button)
4. Share (ghost button)

**Light Mode:** Chat Card replaced by inline static curiosity (no navigation).

**"Play again":**
- In daily: navigates to the next daily (if available) or to Home
- In free play: starts the next puzzle directly

```
│                              │
│    Next puzzle in XX:XX      │
│                              │
└──────────────────────────────┘
```

### Share Format

> **Change (Spec 11):** Puzzles are unique per player (local AI). There is no "puzzle of the day #123". The share highlights streak and performance.

**Win:**

```
Palabrita 🔥12

Challenge 1 — 3/6
🟦🟦🟧⬜⬜
🟦🟦🟦⬜🟦
🟦🟦🟦🟦🟦

💡 1 hint used
```

**Loss:**

```
Palabrita 🔥12

Challenge 2 — X/6
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
Palabrita 🔥12

Free — 4/6
🟦🟧🟥🟥🟥🟥
🟦🟦🟧🟥🟥🟥
🟦🟦🟦🟥🟦🟥
🟦🟦🟦🟦🟦🟦

The word was: GATOS 🐱
💡 2 hints
```

**Details:**
- Header: streak (player identity)
- Context: "Challenge N/3" or "Free"
- Word shown in share (each player has a unique word, no spoiler)
- Category-themed emoji (optional, emoji-free fallback)

## Word Length Progression

Difficulty is implicit via word length (4–6 letters). Players progress through a simple 3-level system (see Spec 15 for full details):

| Level | Word length | Words per batch |
|-------|-------------|-----------------|
| 1 (first gen) | 4 letters | 5 words |
| 2 | 5 letters | 10 words |
| 3+ | 6 letters | 10 words |

Level 1 generates only 5 words of 4 letters (fast first experience). After playing them, the user clicks "Generate More" for Level 2 (10 × 5 letters), then Level 3+ (always 10 × 6 letters). Constants are centralized in `GameRules` object (`core/model`).

Within each batch, puzzles are served in order of generation.

## GameViewModel

### State

```kotlin
data class GameState(
    val puzzle: Puzzle? = null,
    val attempts: List<Attempt> = emptyList(),
    val currentInput: String = "",
    val revealedHints: List<String> = emptyList(),
    val keyboardState: Map<Char, LetterState> = emptyMap(),
    val gameStatus: GameStatus = GameStatus.LOADING,
    val isLoading: Boolean = false,
    val errorRes: Int? = null,
    val gameContext: GameContext = GameContext.FreePlay,
    val showAbandonDialog: Boolean = false,
)

sealed class GameContext {
    data class DailyChallenge(val index: Int, val total: Int = 3) : GameContext()
    data object FreePlay : GameContext()
}

data class Attempt(
    val word: String,
    val feedback: List<LetterFeedback>
)

data class LetterFeedback(
    val letter: Char,
    val state: LetterState
)

enum class LetterState { CORRECT, PRESENT, ABSENT, UNUSED }
enum class GameStatus { PLAYING, WON, LOST, LOADING }
```

### Actions

```kotlin
sealed class GameAction {
    data class TypeLetter(val letter: Char) : GameAction()
    data object DeleteLetter : GameAction()
    data object SubmitAttempt : GameAction()
    data object RevealHint : GameAction()
    data object ShareResult : GameAction()
    data object NavigateToChat : GameAction()
    data object NavigateToStats : GameAction()
    data object LoadNextPuzzle : GameAction()
    data object BackPressed : GameAction()
    data object ConfirmAbandon : GameAction()
    data object DismissAbandonDialog : GameAction()
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

- [ ] Grid renders correctly for words of 4, 5, and 6 letters (3-level system)
- [ ] Color feedback is correct for duplicate letters (per algorithm)
- [ ] Keyboard updates colors correctly after each attempt
- [ ] Flip animation works when revealing feedback
- [ ] Shake animation works for invalid attempt
- [ ] Hints reveal progressively (up to 3)
- [ ] Win correctly detected and navigates to result screen
- [ ] Loss correctly detected after 6 attempts
- [ ] Share generates correct emoji grid (no XP or tier in share text)
- [ ] State persists between app kills (via GameSessionEntity)
- [ ] "Explore the word" only appears in AI mode
- [ ] WorkManager generates puzzles in background when stock is low
- [ ] Header shows "CHALLENGE N/3" for dailies and "FREE" for free play
- [ ] Back button during active game shows "Abandon game?"
- [ ] "Continue playing" closes dialog and returns to game
- [ ] "Abandon" marks GameSession as abandoned and navigates to Home
- [ ] Back without confirmation if game is already over (WON/LOST)
- [ ] Chat Card is the main CTA in ResultScreen (above "play again")
- [ ] Share shows streak in the header
- [ ] Share shows context ("Challenge N/3" or "Free")
- [ ] Accent keyboard toggle appears for PT/ES languages (Spec 17)
- [ ] Accent keys type base letter into input (game logic is ASCII)
