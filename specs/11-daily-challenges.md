# Spec 11 — Daily Challenges

## Summary

The player receives 3 daily challenges with progressive difficulty (easy → normal → hard). Completing challenges unlocks the next one. Maintaining streak requires only 1 game per day. Puzzles are unique per player (local AI), non-deterministic.

## Context & Motivation

Without daily challenges, the app doesn't create habit. Wordle proved that scarcity + ritual = engagement. But 1 puzzle per day limits interaction with the AI Chat (Palabrita's differentiator). With 3 challenges: the player has 3 opportunities to explore words with the AI, difficulty scales naturally within the day, and the completion bonus encourages longer sessions.

## Mechanics

### 3 Challenges per Day

| Challenge | Difficulty | Unlock |
|---------|-------------|-------------|
| ① | shorter word length (easier) | Always available |
| ② | current word length | Complete ① |
| ③ | longer word length (harder) | Complete ② |

**Difficulty is implicit via word length**, controlled by the 3-level generation system (Spec 15). No `currentDifficulty` field — word length determines difficulty (4, 5, or 6 letters).

### Puzzle Selection

```kotlin
suspend fun selectDailyPuzzles(
    language: String,
    currentDifficulty: Int,
    date: LocalDate,
): List<Puzzle> {
    val difficulties = listOf(
        (currentDifficulty - 1).coerceAtLeast(1),
        currentDifficulty,
        (currentDifficulty + 1).coerceAtMost(5),
    )
    
    return difficulties.map { diff ->
        puzzleRepository.getNextUnplayed(language, diff)
            ?: puzzleGenerator.generateInline(language, diff)  // fallback
    }
}
```

- Puzzles are **unique per player** (each one receives different puzzles from the AI or the static database)
- If there are no puzzles in the database for the difficulty, generates inline (AI mode) or looks for adjacent difficulty (Light mode)
- The 3 puzzles are selected when accessing the first daily of the day (lazy — not pre-selected at midnight)

### Sequential Unlock

```
Challenge 1: AVAILABLE from the start of the day
Challenge 2: LOCKED → AVAILABLE when challenge 1 is completed (win or loss)
Challenge 3: LOCKED → AVAILABLE when challenge 2 is completed (win or loss)
```

**Completing = finishing the game** (win or loss). Winning is not required to unlock the next one. The goal is to engage, not to frustrate.

### Flow (no difficulty picker)

```
HomeScreen → tap "PLAY #N" → PlayingScreen (pre-selected puzzle, word length determines difficulty)
                                    │
                                    ▼
                              ResultScreen → AI Chat → Home
```

No difficulty picker. Difficulty is implicit via word length, controlled by generation cycles (Spec 14).

## Rewards

### Completion Bonus (3/3)

Completing all 3 dailies in a day is its own reward — no XP system needed. The streak and completion tracking provide the engagement loop.

## Streak

### Rules

- **Streak increments** when the player **finishes** (win or loss) at least **1 daily** in the day
- **Streak resets** if the player does not finish any daily in a day
- **Free play does NOT count** for streak
- The player does NOT need to win — just participate

### Calculation

```kotlin
fun updateStreak(stats: PlayerStats, now: LocalDate): PlayerStats {
    val lastPlayed = stats.lastPlayedAt?.toLocalDate()
    
    return when {
        lastPlayed == now -> stats // Already played today, no change
        lastPlayed == now.minusDays(1) -> stats.copy(
            currentStreak = stats.currentStreak + 1,
            maxStreak = maxOf(stats.maxStreak, stats.currentStreak + 1),
            lastPlayedAt = now.toEpochMillis(),
        )
        else -> stats.copy(
            currentStreak = 1, // Reset, new streak begins
            lastPlayedAt = now.toEpochMillis(),
        )
    }
}
```

### Streak Milestones

| Days | Milestone |
|------|-------|
| 7 | 🔥 1 week |
| 30 | 🔥🔥 1 month |
| 100 | 🔥🔥🔥 100 days |
| 365 | 🏆 1 year |

> **Note:** XP system was removed. Streak milestones are purely visual — no XP bonuses.

## Sharing

Format (puzzles unique per player, no shared number):

```
Palabrita 🔥12

Challenge 1 — 3/6
🟩🟩🟨⬜⬜
🟩🟩🟩⬜🟩
🟩🟩🟩🟩🟩

Challenge 2 — 4/6
🟩🟨🟨⬜⬜⬜
🟩🟩🟨⬜🟩⬜
🟩🟩🟩⬜🟩🟩
🟩🟩🟩🟩🟩🟩

💡 1 hint used
```

Highlights: streak and performance (no XP or tier — those systems were removed).

## Data Model — Changes

### GameSessionEntity (new fields)

```kotlin
val dailyChallengeIndex: Int?    // 0, 1, 2 = daily; null = free play
val dailyChallengeDate: String?  // "2026-04-21" (ISO date, null = free play)
```

### PlayerStatsEntity (new field)

```kotlin
val lastDailyDate: String?       // "2026-04-21" — last date with a completed daily
```

This separates `lastPlayedAt` (any game) from `lastDailyDate` (daily-specific for streak).

## Daily Reset

- Dailies reset at **local midnight** (`LocalDate.now()`)
- When accessing HomeScreen, checks if `today != lastDailyDate` → new dailies
- Puzzles from previous incomplete dailies are released back to the database
- If the player is playing a daily at 23:59 and finishes at 00:01: the game counts for the day it **started**

## Edge Cases

| Scenario | Behavior |
|---|---|
| Player completes daily 1, closes app, returns tomorrow | Daily 2 and 3 from yesterday not completed; new dailies for the new day |
| Player at tier 1, daily 1 would be tier 0 | `coerceAtLeast(1)` — all 3 are tier 1 |
| Player at tier 5, daily 3 would be tier 6 | `coerceAtMost(5)` — daily 3 is tier 5 |
| No puzzles for the difficulty | AI mode: generate inline. Light mode: look for adjacent difficulty |
| Player loses all 3 dailies | Streak maintained (participated) |
| Free play does not affect streak | Correct — only dailies count |
| App killed during daily | GameSession saved, restored on reopen |

## Decisions

| Decision | Choice | Reason |
|---------|---------|-------|
| Deterministic puzzles | No | Each player has a unique experience (local AI) |
| Unlocking next requires win | No | Completing (win/loss) is enough. Engagement > frustration |
| Daily without difficulty picker | Yes | Difficulty is implicit via word length |
| Streak counts loss | Yes | Played = maintains streak. Rewards participation |
| 3 dailies (not 1) | Yes | 3x AI Chat opportunities + difficulty escalation |

## Out of Scope

- Deterministic / same dailies for all players (decided: no)
- Leaderboard comparing dailies between friends (future)
- Push notification "Your streak is at risk!" (future)
- Replay of previous days' dailies (future)

## Acceptance Criteria

- [ ] 3 daily challenges appear on HomeScreen with correct state
- [ ] Daily 1 = shorter word length (easier)
- [ ] Daily 2 = current word length
- [ ] Daily 3 = longer word length (harder)
- [ ] Daily 2 unlocks when daily 1 is completed (win or loss)
- [ ] Daily 3 unlocks when daily 2 is completed (win or loss)
- [ ] Tap on daily navigates directly to PlayingScreen (no difficulty picker)
- [ ] Completing 3/3 dailies shows completion indicator
- [ ] Streak increments when finishing the 1st daily of the day
- [ ] Streak does NOT increment with free play
- [ ] Streak resets if no daily played the previous day
- [ ] Dailies reset at local midnight
- [ ] `dailyChallengeIndex` and `dailyChallengeDate` saved in GameSession
- [ ] Sharing shows streak (no XP or tier)
- [ ] Streak milestones at 7, 30, 100 and 365 days are purely visual
- [ ] Daily puzzles are unique per player
- [ ] Daily started before midnight counts for the day it started
