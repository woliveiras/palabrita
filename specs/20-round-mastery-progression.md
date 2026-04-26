# Spec: Round Mastery Progression

## Context & Motivation

The current level progression is based on **generation cycles**: after generating a batch of
puzzles, `generationCycle` increments and the next batch uses a harder word length. This means
a player who loses every game at 4-letter words will still be promoted to 5-letter words once
generation triggers again — the system rewards **generation**, not **mastery**.

**Problem scenario:**
1. Level 1 generates 5 words of 4 letters
2. Player loses all 5 games
3. Player clicks "Generate more"
4. System generates 10 words of 5 letters (cycle advanced)
5. Player is now playing harder words they're not ready for

**Desired behavior:**
1. Level 1 generates 5 words of 4 letters
2. Player wins 3, loses 2
3. Player still needs 2 more wins → system generates more 4-letter words
4. Player wins 2 more → total 5 wins at 4 letters
5. NOW the system generates 5-letter words (level 2)

The progression gate is **wins**, not **generation count**.

## How It Works

### Mastery Gate

Each level has a `winsRequired` threshold. The player must accumulate that many wins
at the level's word length before the system will generate words at the next level.

| Level | Word length | Initial batch | Wins required | Refill batch |
|-------|-------------|---------------|---------------|--------------|
| 1     | 4 letters   | 5 words       | 5 wins        | remaining¹   |
| 2     | 5 letters   | 10 words      | 10 wins       | remaining¹   |
| 3+    | 6 letters   | 10 words      | 10 wins       | 10 words     |

¹ `remaining = winsRequired - currentWins`, capped at batch size

### Generation Flow (revised)

```
1. User triggers generation (or runs out of puzzles)
2. Read current cycle from AppPreferences
3. Get level config: (wordLength, batchSize, winsRequired)
4. Count wins at this difficulty: JOIN game_sessions + puzzles WHERE won=1 AND difficulty=wordLength AND language=lang
5. If wins >= winsRequired:
   a. Increment generationCycle → move to next level
   b. Re-read level config for new cycle
   c. Generate batch at new level's wordLength and batchSize
6. If wins < winsRequired:
   a. Do NOT increment generationCycle
   b. remaining = winsRequired - wins
   c. Generate min(remaining, batchSize) words at SAME wordLength
```

### Puzzle Selection (unchanged)

`PuzzleDao.getNextUnplayed(lang)` already orders by `difficulty ASC, RANDOM()`.
This naturally serves lower-difficulty puzzles first, which aligns with mastery
progression — the player finishes 4-letter puzzles before seeing 5-letter ones.

### What Counts as a Win

A win is a completed `GameSession` where `won = true`, linked to a `Puzzle` with the
matching `difficulty` (word length) and `language`. Abandoned games don't count.

## Requirements

### Functional

- [ ] Track wins per difficulty level using existing `game_sessions` + `puzzles` tables (JOIN query)
- [ ] Gate level progression on wins: player must reach `winsRequired` at current difficulty before advancing
- [ ] When wins < required and puzzles run out, generate more words at the SAME difficulty
- [ ] When wins >= required, increment `generationCycle` and generate at the NEXT difficulty
- [ ] Refill batch size = `min(winsRequired - currentWins, batchSize)` when staying at same level
- [ ] Level 3+ (6-letter words) is the cap — always generates 6-letter words, winsRequired = 10
- [ ] `GameRules.GENERATION_LEVELS` updated to include `winsRequired` per level
- [ ] New `GameSessionDao` query: `countWinsByDifficulty(difficulty, language)` using JOIN
- [ ] New `GameSessionRepository` method: `countWinsByDifficulty(difficulty, language): Int`
- [ ] `GeneratePuzzlesUseCase` uses mastery gate logic instead of unconditional cycle increment

### Non-Functional

- [ ] Zero Room schema migration — uses existing tables and a new JOIN query
- [ ] No new DataStore keys — reuses `generationCycle`
- [ ] Win count query must be fast (indexed on `game_sessions.puzzleId` + `puzzles.difficulty`)
- [ ] Backward compatible: existing players keep their `generationCycle` value; wins already recorded in `game_sessions` are counted retroactively

## Acceptance Criteria

### Win Counting

- [ ] Given 3 completed games with won=true for difficulty=4 and language="pt", when `countWinsByDifficulty(4, "pt")` is called, then it returns 3
- [ ] Given 5 games where 3 won and 2 lost for difficulty=4, when counting wins, then result is 3 (losses not counted)
- [ ] Given wins at difficulty=4 in "pt" and difficulty=4 in "en", when counting wins for "pt", then only "pt" wins are counted
- [ ] Given no completed games, when counting wins, then result is 0
- [ ] Given an abandoned game (completedAt=null), when counting wins, then it is not counted

### Mastery Gate — Level 1

- [ ] Given cycle=0 (level 1, 4-letter, winsRequired=5) and 4 wins at difficulty=4, when generation triggers, then it generates more 4-letter words and does NOT increment cycle
- [ ] Given cycle=0 and 5 wins at difficulty=4, when generation triggers, then it increments cycle to 1 and generates 5-letter words
- [ ] Given cycle=0 and 0 wins and 0 unplayed puzzles, when generation triggers, then it generates 5 words of 4 letters (full initial batch)
- [ ] Given cycle=0, 3 wins, and 0 unplayed puzzles, when generation triggers, then it generates 2 words of 4 letters (remaining = 5 - 3)

### Mastery Gate — Level 2

- [ ] Given cycle=1 (level 2, 5-letter, winsRequired=10) and 7 wins at difficulty=5, when generation triggers, then it generates 3 more 5-letter words (remaining = 10 - 7)
- [ ] Given cycle=1 and 10 wins at difficulty=5, when generation triggers, then it increments cycle to 2 and generates 6-letter words

### Mastery Gate — Level 3 (cap)

- [ ] Given cycle=2 (level 3, 6-letter, winsRequired=10) and 10 wins at difficulty=6, when generation triggers, then cycle increments to 3 but level config still returns 6-letter words (capped)
- [ ] Given cycle=5 and 8 wins at difficulty=6, when generation triggers, then it generates 2 more 6-letter words (remaining = 10 - 8, still capped at level 3)

### Refill Batch Size

- [ ] Given winsRequired=5 and currentWins=3, when computing refill batch, then result is 2
- [ ] Given winsRequired=10 and currentWins=0, when computing refill batch, then result is 10 (capped at batchSize)
- [ ] Given winsRequired=10 and currentWins=9, when computing refill batch, then result is 1
- [ ] Given winsRequired=5 and currentWins=5, when computing refill batch, then no refill happens (level advances instead)

### Replenishment Threshold

- [ ] Given unplayed puzzles >= REPLENISHMENT_THRESHOLD, when generation triggers, then it is skipped (existing behavior preserved)

### Backward Compatibility

- [ ] Given an existing user with cycle=2 and historical wins, when they trigger generation, their existing wins at difficulty=6 are counted toward the mastery gate
- [ ] Given an existing user with cycle=1 but 15 wins at difficulty=5 already, when generation triggers, then they advance immediately (retroactive credit)

## Data Model Changes

### New Query (GameSessionDao)

```kotlin
@Query("""
    SELECT COUNT(*) FROM game_sessions gs
    INNER JOIN puzzles p ON gs.puzzleId = p.id
    WHERE gs.won = 1
      AND gs.completedAt IS NOT NULL
      AND p.difficulty = :difficulty
      AND p.language = :language
""")
suspend fun countWinsByDifficulty(difficulty: Int, language: String): Int
```

### Updated GameRules

```kotlin
data class Level(val wordLength: Int, val batchSize: Int, val winsRequired: Int)

object GameRules {
  val GENERATION_LEVELS = listOf(
    Level(wordLength = 4, batchSize = 5, winsRequired = 5),
    Level(wordLength = 5, batchSize = 10, winsRequired = 10),
    Level(wordLength = 6, batchSize = 10, winsRequired = 10),
  )

  fun levelForCycle(cycle: Int): Level =
    GENERATION_LEVELS[cycle.coerceIn(0, GENERATION_LEVELS.lastIndex)]
}
```

### Updated GeneratePuzzlesUseCase (pseudocode)

```kotlin
val cycle = appPreferences.generationCycle.first()
val level = GameRules.levelForCycle(cycle)
val wins = gameSessionRepository.countWinsByDifficulty(level.wordLength, language)

if (wins >= level.winsRequired) {
    appPreferences.incrementGenerationCycle()
    val nextLevel = GameRules.levelForCycle(cycle + 1)
    generate(nextLevel.wordLength, nextLevel.batchSize)
} else {
    val remaining = (level.winsRequired - wins).coerceAtMost(level.batchSize)
    generate(level.wordLength, remaining)
}
```

## Edge Cases

- Player has 5 wins from 4-letter words but also 3 wins from old 5-letter words (from before this spec) → both are counted; when cycle advances to level 2, those 3 wins give a head start
- Wordlist exhausted for a difficulty → `WordListProvider.pickWords` returns fewer words; partial batch is saved, player can still win those and eventually advance
- Player changes language → wins are per-language; switching to "en" resets effective progress since they have 0 wins at difficulty=4 in "en"
- Multiple generation triggers with same cycle → idempotent; won't over-generate because `REPLENISHMENT_THRESHOLD` prevents it if enough unplayed puzzles exist
- Level 3+ keeps cycling `generationCycle` (3, 4, 5…) but `levelForCycle` always returns the capped level (6-letter, 10 batch, 10 wins) — every 10 wins generates another batch of 6-letter words indefinitely

## Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Win tracking | JOIN query on existing tables | Zero migration; data already exists |
| Mastery gate | Per-level `winsRequired` | Prevents premature promotion; ensures player learns at each level |
| Refill batch size | `min(remaining, batchSize)` | Don't over-generate; create only what's needed to reach mastery |
| Wordlist in SQLite | No | Filtering via `allExistingWords` already prevents repeats; no duplication needed |
| Retroactive wins | Yes | Existing players shouldn't lose progress; historical wins count |
| Level 3 cap behavior | Cycle still increments | Keeps counting, but config is always 6-letter; simplifies logic |
| winsRequired = batchSize | Yes (for levels 1-2) | Natural: "win all puzzles in the batch to advance" is intuitive |

## Out of Scope

- UI showing mastery progress (e.g., "3/5 wins at this level") — can be added later
- Adaptive difficulty within a level (e.g., easier hints after losses)
- XP or tier system
- Changing the game mechanics (attempts, hints, feedback)
- Static puzzle dataset seeding (Spec 09)
- Daily challenges interaction with mastery (Spec 11)
