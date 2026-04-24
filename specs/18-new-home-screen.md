# Spec 18 — New Home Screen

## Context & Motivation

The current HomeScreen is minimal — a title, two stat numbers, and a Play button. The new design is a proper home hub with clear visual hierarchy: header with settings, a prominent Play CTA card showing available puzzles, three stat cards with icons, and quick-access cards for "How to Play" and "About AI Model".

This replaces Spec 10 (which referenced daily challenges, streak, and features that no longer exist).

Reference: [Screenshot — Palabrita UI Design](../Screenshots/home-design.png)

## Layout

```
┌──────────────────────────────────┐
│  Palabrita              ⚙️       │  ← Header: app name + settings icon
│  🇧🇷 Português • Gemma 4 2B     │  ← Subtitle: language flag + model name
│                                  │
│  ┌────────────────────────────┐  │
│  │  ✨                        │  │
│  │  Play Now            ▶    │  │  ← Main CTA Card (gradient background)
│  │  5 puzzles available       │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌────────┐ ┌────────┐ ┌──────┐ │
│  │ ▷  24  │ │ 🏆 83% │ │📈  7 │ │  ← Quick Stats Row (3 cards)
│  │ Games  │ │  Win   │ │Curr. │ │
│  │ Played │ │  Rate  │ │Streak│ │
│  └────────┘ └────────┘ └──────┘ │
│                                  │
│  ┌────────────────────────────┐  │
│  │  ❓ How to Play            │  │  ← Info Card 1
│  │     Learn the rules        │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │  ℹ️ About AI Model         │  │  ← Info Card 2
│  │     See how it works       │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌──────────────────────────────┐│
│  │ 🏠 Home  │ 🤖 AI  │ ⚙️ More ││  ← Bottom Nav (existing)
│  └──────────────────────────────┘│
└──────────────────────────────────┘
```

## Requirements

### Functional

- [ ] Header shows "Palabrita" as app name (left-aligned, bold)
- [ ] Header shows a settings gear icon (top-right) that navigates to SettingsScreen
- [ ] Subtitle shows language flag emoji + language name + model display name (e.g., "🇧🇷 Português • Gemma 4 2B")
- [ ] Main CTA card has gradient background (purple/violet, matching design)
- [ ] Main CTA card shows "Play Now" with a play icon
- [ ] Main CTA card shows "{N} puzzles available" count
- [ ] Tapping CTA card navigates to GameScreen
- [ ] When no puzzles are available, CTA card shows "Generate More" and navigates to GenerationScreen
- [ ] Quick Stats row shows 3 cards side by side: Games Played, Win Rate, Current Streak
- [ ] Each stat card has an icon, a large value, and a label
- [ ] Current Streak is calculated from consecutive won games (most recent first)
- [ ] "How to Play" card navigates to a rules bottom sheet or dialog
- [ ] "About AI Model" card navigates to AiInfoScreen (existing route)
- [ ] Generation-in-progress indicator shown when WorkManager is running (small, non-blocking)

### Non-Functional

- [ ] Performance: HomeScreen loads in under 100ms (stats from Room, no network)
- [ ] Accessibility: all cards have content descriptions; stat values are announced
- [ ] Responsiveness: layout scrolls vertically on smaller screens

## Data Sources

| UI Element | Data Source | Field |
|---|---|---|
| Language flag + name | `PlayerStats.preferredLanguage` | Map: "pt" → "🇧🇷 Português", "en" → "🇺🇸 English", "es" → "🇪🇸 Español" |
| Model display name | `ModelConfig.modelId` → `AiModelRegistry.getInfo()` | `displayName` |
| Puzzles available | `PuzzleRepository.countAllUnplayed(language)` | Int count |
| Games Played | `PlayerStats.totalPlayed` | Int |
| Win Rate | `PlayerStats.totalWon / totalPlayed` | Float → percentage |
| Current Streak | `GameSessionDao` — count consecutive `won=true` ordered by `completedAt DESC` | Int |
| Generation running | `WorkManager.getWorkInfosForUniqueWork(...)` | WorkInfo.State |

## Current Streak Calculation

```kotlin
// New DAO query — count consecutive wins from most recent game backwards
@Query("""
  SELECT COUNT(*) FROM (
    SELECT won FROM game_sessions 
    WHERE completedAt IS NOT NULL 
    ORDER BY completedAt DESC
  ) WHERE won = 1
""")
// Note: SQLite stops at first non-win row. Use a CTE or compute in Kotlin:

// Kotlin approach (simpler, reliable):
fun calculateStreak(sessions: List<GameSession>): Int {
    return sessions
        .sortedByDescending { it.completedAt }
        .takeWhile { it.won }
        .count()
}
```

## State

```kotlin
data class HomeState(
  val isLoading: Boolean = true,
  // Header
  val languageDisplay: String = "",     // "🇧🇷 Português • Gemma 4 2B"
  // CTA
  val unplayedCount: Int = 0,
  val isGeneratingPuzzles: Boolean = false,
  val generationComplete: Boolean = false,
  // Stats
  val totalPlayed: Int = 0,
  val winRate: Float = 0f,
  val currentStreak: Int = 0,
)
```

## Actions

```kotlin
sealed class HomeAction {
  data object Play : HomeAction()
  data object GenerateMore : HomeAction()
  data object OpenSettings : HomeAction()
  data object OpenHowToPlay : HomeAction()
  data object OpenAboutAi : HomeAction()
  data object DismissGenerationBanner : HomeAction()
}
```

## Edge Cases

| Scenario | Behavior |
|---|---|
| No games played yet | Stats show 0, 0%, 0 — still valid |
| No puzzles available + not generating | Show "Generate More" CTA |
| No puzzles available + generation in progress | Show disabled CTA + generation indicator |
| Model is NONE (shouldn't happen post-onboarding) | Subtitle shows language only, no model name |
| All games lost | Current Streak = 0 |
| 100% win rate | Current Streak = totalPlayed |

## Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Subtitle content | Language + model name | Replaces removed XP/Level; shows useful context |
| CTA card style | Gradient purple Card | Matches design mockup; high visual prominence |
| Streak vs Avg Attempts in stats | Streak | User chose it; more motivating than avg attempts |
| Streak calculation | Kotlin-side from DAO list | Simpler than complex SQL CTE; few sessions in memory |
| "How to Play" target | Bottom sheet dialog | Lightweight, no new screen needed |
| "About AI Model" target | Existing AiInfoRoute | Already implemented |

## Out of Scope

- Daily Challenges (removed from project)
- XP / Level system (removed from project)
- Light mode differences (removed from project)
- Streak persistence in PlayerStats (calculate on-the-fly for now)
- Animations on stat changes (future enhancement)

## Acceptance Criteria

- [ ] Given the home screen loads, when the user has played 24 games with 83% win rate, then stats show "24", "83%", and correct streak
- [ ] Given there are 5 unplayed puzzles, when the home screen loads, then CTA shows "Play Now" with "5 puzzles available"
- [ ] Given there are 0 unplayed puzzles, when the home screen loads, then CTA shows "Generate More"
- [ ] Given the user's language is "pt" and model is GEMMA4_E2B, then subtitle shows "🇧🇷 Português • Gemma 4 2B"
- [ ] Given the user won the last 7 games in a row, then Current Streak shows 7
- [ ] Given the user lost the most recent game but won 5 before that, then Current Streak shows 0
- [ ] Given the user taps the Play CTA, then navigation goes to GameScreen
- [ ] Given the user taps the settings icon, then navigation goes to SettingsScreen
- [ ] Given the user taps "How to Play", then a rules dialog/bottom sheet appears
- [ ] Given the user taps "About AI Model", then navigation goes to AiInfoScreen
- [ ] Given puzzle generation is running, then a small indicator is visible on the home screen
- [ ] Given a new user with no games, then all stats show 0 and CTA shows available puzzle count
