# Spec 11 — Daily Challenges

## Summary

The player receives 3 daily challenges with progressive difficulty (easy → normal → hard). Completing challenges unlocks the next one, generates XP with a 2x bonus, and maintaining streak requires only 1 game per day. Puzzles are unique per player (local AI), non-deterministic.

## Context & Motivation

Without daily challenges, the app doesn't create habit. Wordle proved that scarcity + ritual = engagement. But 1 puzzle per day limits interaction with the AI Chat (Palabrita's differentiator). With 3 challenges: the player has 3 opportunities to explore words with the AI, difficulty scales naturally within the day, and the completion bonus encourages longer sessions.

## Mechanics

### 3 Challenges per Day

| Challenge | Difficulty | Unlock |
|---------|-------------|-------------|
| ① | current tier - 1 (min 1) | Always available |
| ② | current tier | Complete ① |
| ③ | current tier + 1 (max 5) | Complete ② |

**Where "current tier" = `currentDifficulty` from `PlayerStats`** (existing adaptive difficulty, spec 05).

Examples:
- Player level 1: challenges ⭐/⭐/⭐⭐
- Player level 3: challenges ⭐⭐/⭐⭐⭐/⭐⭐⭐⭐
- Player level 5: challenges ⭐⭐⭐⭐/⭐⭐⭐⭐⭐/⭐⭐⭐⭐⭐

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

### Flow (without DifficultyPicker)

```
HomeScreen → tap "PLAY #N" → PlayingScreen (pre-selected puzzle, automatic difficulty)
                                    │
                                    ▼
                              ResultScreen → AI Chat → Home
```

Does not go through DifficultyPicker. Difficulty is automatic (progressive).

## XP and Rewards

### XP per Daily Challenge

```kotlin
fun calculateDailyXp(
    won: Boolean,
    attempts: Int,
    difficulty: Int,
    hintsUsed: Int,
    currentStreak: Int,
): Int {
    val baseXp = calculateXpForGame(won, attempts, difficulty, currentStreak, hintsUsed)
    return baseXp * 2  // 2x bonus for dailies
}
```

- Dailies give **2x XP** compared to the same game in free play
- The streak bonus (spec 05) applies normally

### Completion Bonus (3/3)

```kotlin
fun calculateCompletionBonus(
    dailyResults: List<DailyChallengeResult>,
): Int {
    if (dailyResults.size < 3 || !dailyResults.all { it.completed }) return 0
    
    val winsCount = dailyResults.count { it.won }
    return when (winsCount) {
        3 -> 5   // Swept: +5 XP bonus
        2 -> 3   // Almost: +3 XP bonus
        1 -> 1   // Persistent: +1 XP bonus
        0 -> 1   // Dedicated: +1 XP (participated in all 3)
        else -> 0
    }
}
```

- Completing all 3 (win or loss) = extra bonus
- The more wins in the 3, the bigger the bonus
- Even losing all 3, get +1 XP bonus for having tried all

### Daily Maximum XP Summary

| Source | XP |
|-------|-----|
| Daily 1 (easy, 1st-attempt win) | (1+3) × 2 = 8 |
| Daily 2 (normal, 1st-attempt win) | (2+3) × 2 = 10 |
| Daily 3 (hard, 1st-attempt win) | (3+3) × 2 = 12 |
| Completion bonus (3 wins) | 5 |
| AI Chat bonus (3 sessions) | 3 |
| **Theoretical max/day** | **38 XP** |

In practice, ~15-20 XP/day is more realistic (not always 1st attempt).

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

| Days | Milestone | Bonus |
|------|-------|-------|
| 7 | 🔥 1 week | +5 XP |
| 30 | 🔥🔥 1 month | +20 XP |
| 100 | 🔥🔥🔥 100 days | +50 XP (new!) |
| 365 | 🏆 1 year | +100 XP + "Legendary" badge |

## Sharing

Format (puzzles unique per player, no shared number):

```
Palabrita 🔥12 · Savvy · 350 XP

Challenge 1 ⭐ — 3/6
🟩🟩🟨⬜⬜
🟩🟩🟩⬜🟩
🟩🟩🟩🟩🟩

Challenge 2 ⭐⭐ — 4/6
🟩🟨🟨⬜⬜⬜
🟩🟩🟨⬜🟩⬜
🟩🟩🟩⬜🟩🟩
🟩🟩🟩🟩🟩🟩

💡 1 hint used · +22 XP today
```

Highlights: streak, tier and XP (player identity, not puzzle).

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
| Player loses all 3 dailies | Streak maintained (participated). XP = 0 from games + 1 completion bonus |
| Free play does not affect streak | Correct — only dailies count |
| App killed during daily | GameSession saved, restored on reopen |

## Decisions

| Decision | Choice | Reason |
|---------|---------|-------|
| Deterministic puzzles | No | Each player has a unique experience (local AI) |
| Unlocking next requires win | No | Completing (win/loss) is enough. Engagement > frustration |
| Daily without DifficultyPicker | Yes | Automatic difficulty simplifies flow |
| Streak counts loss | Yes | Played = maintains streak. Rewards participation |
| 3 dailies (not 1) | Yes | 3x AI Chat opportunities + difficulty escalation |

## Out of Scope

- Deterministic / same dailies for all players (decided: no)
- Leaderboard comparing dailies between friends (future)
- Push notification "Your streak is at risk!" (future)
- Replay of previous days' dailies (future)

## Acceptance Criteria

- [ ] 3 daily challenges appear on HomeScreen with correct state
- [ ] Difficulty of daily 1 = `currentDifficulty - 1` (min 1)
- [ ] Difficulty of daily 2 = `currentDifficulty`
- [ ] Difficulty of daily 3 = `currentDifficulty + 1` (max 5)
- [ ] Daily 2 unlocks when daily 1 is completed (win or loss)
- [ ] Daily 3 unlocks when daily 2 is completed (win or loss)
- [ ] Tap on daily navigates directly to PlayingScreen (no DifficultyPicker)
- [ ] Daily gives 2x XP compared to free play
- [ ] Completing 3/3 dailies gives extra XP bonus
- [ ] Streak increments when finishing the 1st daily of the day
- [ ] Streak does NOT increment with free play
- [ ] Streak resets if no daily played the previous day
- [ ] Dailies reset at local midnight
- [ ] `dailyChallengeIndex` and `dailyChallengeDate` saved in GameSession
- [ ] Sharing shows streak + tier + XP (not puzzle number)
- [ ] Streak bonus at 7, 30, 100 and 365 days works
- [ ] Daily puzzles are unique per player
- [ ] Daily started before midnight counts for the day it started
