# Spec: Simplified Level-Based Generation

## Context & Motivation

The current cycle-based generation system generates batches of 20 puzzles distributed
across multiple word lengths (4–8), which is slow and complex. The generation loop
iterates through several word lengths per batch, increasing wait time and LLM failures.

**Simplification approach:**
- Each generation produces words of a **single length** — no more mixed-length loops
- Only 3 levels, capping at 6-letter words (not 8)
- First generation is intentionally small (5 words) so the user starts playing fast
- After level 3, always generates 10 words of 6 letters
- User clicks "Generate more" when they run out of puzzles

## Level Progression

| Level | Word length | Batch size | When |
|-------|-------------|------------|------|
| 1     | 4 letters   | 5 words    | First generation (onboarding) |
| 2     | 5 letters   | 10 words   | Second generation |
| 3+    | 6 letters   | 10 words   | Third generation and every subsequent one |

**Key properties:**
- Each generation is a single word length — no loops across lengths
- Level 1 is small (5 words) so the user doesn't wait long during onboarding
- Level 3 is the cap — 6 letters is the hardest it gets
- The `generationCycle` counter in `AppPreferences` tracks which level the user is on
- `difficulty` column in DB stores the word length (4, 5, or 6)
- Play order: ascending by `difficulty` (word length), then by `id`

## Requirements

### Functional

- [x] Level 1 (cycle 0): generate exactly 5 words of 4 letters
- [x] Level 2 (cycle 1): generate exactly 10 words of 5 letters
- [x] Level 3+ (cycle ≥ 2): generate exactly 10 words of 6 letters
- [x] Each generation produces words of a single length — no mixed-length loops
- [x] Max word length is 6 (down from 8)
- [x] Batch size is 5 for level 1, 10 for all others
- [x] `generationCycle` counter increments after each successful generation
- [x] Puzzles are served in ascending word-length order, then by id
- [x] Language is determined by `stats.preferredLanguage` (changeable in Settings)
- [x] When all puzzles are played, user sees option to generate more

### Non-Functional

- [x] Level 1 generation should be noticeably faster (5 words vs 20)
- [x] Simpler generation loop — single word length per batch reduces failure points

## Acceptance Criteria

### Worker Logic

- [x] Given cycle 0, when generation starts, then it requests 5 words of 4 letters
- [x] Given cycle 1, when generation starts, then it requests 10 words of 5 letters
- [x] Given cycle 2, when generation starts, then it requests 10 words of 6 letters
- [x] Given cycle 5 (any value ≥ 2), when generation starts, then it requests 10 words of 6 letters
- [x] Given a completed generation, when all words are saved, then generationCycle increments by 1

### Batch Size

- [x] Given cycle 0, when computing batch size, then result is 5
- [x] Given cycle 1, when computing batch size, then result is 10
- [x] Given cycle 2+, when computing batch size, then result is 10

### Word Length

- [x] Given cycle 0, when computing word length, then result is 4
- [x] Given cycle 1, when computing word length, then result is 5
- [x] Given cycle 2+, when computing word length, then result is 6
- [x] Given any cycle, when generation runs, then all generated words have the same length

### Play Order

- [x] Given puzzles with mixed word lengths (from prior levels), when serving next puzzle, then shorter words come first

### Integration

- [x] Given user changed language to "en" in Settings, when generation runs, then words are generated in English
- [x] Given unplayed count ≥ threshold, when worker starts, then it skips generation (existing behavior)

## Edge Cases

- User has old puzzles with word lengths 7–8 from previous system → they still work, served first (shorter lengths served before longer)
- User reinstalls app → cycle resets to 0, starts from level 1
- LLM fails to generate all words in a batch → partial batch is saved, cycle still increments if at least 1 word generated
- User changes language between generations → new words are in the new language; old words in old language are still playable

## Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Max word length | 6 letters | Simpler for LLM, accessible for players, avoids rare words |
| Level 1 batch size | 5 words | Fast onboarding — user starts playing in seconds |
| Level 2+ batch size | 10 words | Good balance between wait time and content |
| Single length per generation | Yes | Eliminates the mixed-length loop, simplifies worker logic |
| Level cap | Level 3 (6 letters) | 3 levels is simple; 6-letter words are challenging enough |
| Cycle counter reuse | `AppPreferences.generationCycle` | Already exists, no new storage needed |

## Out of Scope

- Changing the game UI or hint display
- Room database migration
- XP system or tier display
- UI for showing player level (can be added later based on cycle count)
- Rebalancing replenishment threshold
