# Spec 10 — HomeScreen

## Summary

The HomeScreen is the central hub of Palabrita. It replaces the current flow (DifficultyPicker as the initial screen) with a hub that shows streak, daily challenges, free play, quick stats, and a puzzle generation indicator. All navigation starts here and returns here.

## Context & Motivation

Today the app opens directly on the DifficultyPicker — no context, no sense of progress, no ritual. Successful games (Wordle, Words of Wonders, Wordscapes) use a hub screen that shows the player's state before starting. The HomeScreen solves this and creates the daily ritual: open → see streak → play → explore words → share.

## Navigation

```
App Launch
  ├── 1st time → Onboarding → HomeScreen
  └── Already completed → HomeScreen

HomeScreen
  ├── Daily Challenges → PlayingScreen (no DifficultyPicker)
  ├── Free Play → DifficultyPicker → PlayingScreen
  ├── Bottom Nav: Stats → StatsScreen
  └── Bottom Nav: More → SettingsScreen
```

The HomeScreen is the `startDestination` after onboarding. The current `GameRoute` becomes `HomeRoute`. The `DifficultyPicker` becomes accessible only via Free Play.

## Layout

```
┌──────────────────────────────────┐
│    P A L A B R I T A             │  ← TopBar: centered logo
├──────────────────────────────────┤
│                                  │
│  ┌────────────────────────────┐  │
│  │  🔥 7-day streak!          │  │  ← Streak Card
│  │  ████████░░  Next: 🏆      │  │     Visual bar, next milestone
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │  ⭐ DAILY CHALLENGES (1/3) │  │  ← Daily Challenges Card
│  │                            │  │
│  │  ① ✅ Animals  3/6         │  │
│  │  ② 🔓 Food     ⭐⭐         │  │
│  │  ③ 🔒 ???      ⭐⭐⭐       │  │
│  │                            │  │
│  │       [ PLAY #2 ]          │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │  🎲 FREE PLAY              │  │  ← Free Play Card
│  │  Choose difficulty and     │  │
│  │  play as many times        │  │
│  │  as you want               │  │
│  │       [ PLAY ]             │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌──────────┬──────────┐        │
│  │ 42 games │ 87% wins │        │  ← Quick Stats
│  │ 🏆 Savvy │ 350 XP   │        │
│  └──────────┴──────────┘        │
│                                  │
│  ┌────────────────────────────┐  │  ← Generation Indicator (if active)
│  │  ⟳ Generating new puzzles… │  │
│  └────────────────────────────┘  │
│                                  │
├──────────────────────────────────┤
│  🏠 Home   📊 Stats   ⚙️ More  │  ← Bottom Navigation
└──────────────────────────────────┘
```

## Components

### StreakCard

- Shows `currentStreak` in days
- Visual progress bar to the next milestone (7, 30, 100 days)
- If streak = 0: "Start your streak today!"
- If streak > 0: "🔥 {N}-day streak!"

### DailyChallengesCard

- Shows 3 challenges with individual progress
- Each challenge has: number (①②③), state (✅/🔓/🔒), category (teaser), difficulty (stars)
- Challenge 1: always unlocked
- Challenge 2: unlocked when 1 is completed
- Challenge 3: unlocked when 2 is completed
- CTA: "PLAY #N" points to the next uncompleted challenge
- When all 3 are completed: "✓ 3/3 complete! +bonus XP"
- If the player used AI chat after a challenge: "✅ Completed · 💬 Explored"
- If NOT used chat: "✅ Completed · 💬 Explore?" (link to chat)
- Daily reset: at local midnight, the 3 challenges reset

### FreePlayCard

- Simple card with "FREE PLAY" + description + CTA "PLAY"
- Tap → navigates to DifficultyPicker (existing flow)

### QuickStatsRow

- 2×2 compact grid: total games, win rate, tier, XP
- Tap on any stat → navigates to StatsScreen

### GenerationIndicator

- Visible **only** when WorkManager is generating puzzles in the background
- Text: "⟳ Generating new puzzles…"
- When done: transitions to "✓ New puzzles ready!" (auto-dismiss after 3s)
- In Light mode: never appears
- **State source**: `WorkManager.getWorkInfoByIdLiveData()` or Flow from `PuzzleGenerationScheduler`

### ChatNudge (conditional)

- Appears if the player completed a challenge but did NOT use AI chat
- Text: "Want to learn more about 'CATS'? [Explore now]"
- Tap → navigates to ChatRoute(puzzleId)
- Dismiss: "✕" in the corner
- Only appears in AI mode

### BottomNavigation

- 3 tabs: Home (🏠), Stats (📊), More (⚙️)
- Home = HomeScreen (this)
- Stats = StatsScreen (existing)
- More = SettingsScreen (existing, includes profile and about)
- Visual indicator on the active tab

## ViewModel

### HomeState

```kotlin
data class HomeState(
    val streak: Int,
    val nextStreakMilestone: Int,           // 7, 30, 100
    val dailyChallenges: List<DailyChallenge>,
    val completedDailies: Int,             // 0, 1, 2, 3
    val allDailiesComplete: Boolean,
    val totalPlayed: Int,
    val winRate: Float,
    val playerTier: String,
    val totalXp: Int,
    val isGeneratingPuzzles: Boolean,
    val generationComplete: Boolean,
    val chatNudge: ChatNudge?,             // null if not applicable
)

data class DailyChallenge(
    val index: Int,                        // 0, 1, 2
    val state: DailyChallengeState,        // LOCKED, AVAILABLE, COMPLETED
    val difficulty: Int,                   // 1-5
    val categoryHint: String?,             // "Animals" (teaser, null if locked)
    val result: DailyChallengeResult?,     // attempts, chat used (null if not completed)
    val puzzleId: Long?,                   // for navigating to chat
)

enum class DailyChallengeState { LOCKED, AVAILABLE, COMPLETED }

data class DailyChallengeResult(
    val attempts: Int,
    val won: Boolean,
    val chatExplored: Boolean,
)

data class ChatNudge(
    val word: String,
    val puzzleId: Long,
)
```

### HomeAction

```kotlin
sealed class HomeAction {
    data class StartDailyChallenge(val index: Int) : HomeAction()
    data object StartFreePlay : HomeAction()
    data object DismissChatNudge : HomeAction()
    data class NavigateToChat(val puzzleId: Long) : HomeAction()
    data object DismissGenerationBanner : HomeAction()
}
```

## Data — Daily Challenges Tracking

### New field in GameSessionEntity

```kotlin
// Add to existing GameSessionEntity:
val dailyChallengeIndex: Int?   // 0, 1, 2 for dailies; null for free play
val dailyChallengeDate: String? // "2026-04-21" (ISO date to identify which day it belongs to)
```

### DailyChallengeDao (queries)

```kotlin
@Query("""
    SELECT * FROM game_sessions 
    WHERE dailyChallengeDate = :date 
    ORDER BY dailyChallengeIndex
""")
suspend fun getDailyChallengesForDate(date: String): List<GameSessionEntity>

@Query("""
    SELECT COUNT(*) FROM game_sessions 
    WHERE dailyChallengeDate = :date AND completedAt IS NOT NULL
""")
suspend fun countCompletedDailies(date: String): Int
```

## Edge Cases

| Scenario | Behavior |
|---|---|
| First access (no stats) | Streak = 0, "Start your streak!", dailies available |
| All dailies done | Card shows "3/3 complete! +bonus", CTA disappears |
| No puzzles in database (daily) | Generate inline via PuzzleGenerator; if Light: static fallback |
| Midnight while using the app | Dailies reset on next Home access (not mid-screen) |
| App opened at 23:59, closes at 00:01 | On returning to Home, new day's dailies |
| Light mode with no AI | Chat nudge never appears; static curiosity card in result |
| Background generation fails | Indicator disappears; static puzzles serve as fallback |
| Player returns from PlayingScreen (back) | Home updates dailies state |

## Decisions

| Decision | Choice | Reason |
|---------|---------|-------|
| DifficultyPicker in daily | Removed | Difficulty is automatic (progressive) |
| Bottom nav vs hamburger | Bottom nav (3 tabs) | Android standard, more accessible |
| Streak trigger | 1st daily game completed | Minimum effort to maintain streak |
| Generation feedback | Indicator on Home | Transparency without interrupting |

## Out of Scope

- Leaderboard / friends (future)
- Profile with customizable avatar (future)
- Temporary / seasonal events (future)
- Push notifications for streak at risk (future)

## Acceptance Criteria

- [ ] HomeScreen is the initial screen after onboarding
- [ ] Streak card shows `currentStreak` and progress bar
- [ ] Daily Challenges card shows 3 challenges with correct state (locked/available/completed)
- [ ] Challenge 2 only unlocks after completing challenge 1
- [ ] Challenge 3 only unlocks after completing challenge 2
- [ ] Tap on "PLAY #N" navigates to PlayingScreen with the daily puzzle (no DifficultyPicker)
- [ ] After completing all 3 dailies, card shows bonus
- [ ] Daily card shows "💬 Explore?" if player did not use chat (AI mode)
- [ ] Free Play card navigates to DifficultyPicker
- [ ] Quick stats shows total games, win rate, tier and XP
- [ ] Generation indicator appears when WorkManager is generating and disappears when done
- [ ] Chat nudge appears if last daily completed without chat (AI mode)
- [ ] Bottom Navigation with 3 tabs works correctly
- [ ] Dailies reset at local midnight
- [ ] HomeScreen updates when returning from a game
- [ ] Light mode does not show chat nudge or generation indicator
