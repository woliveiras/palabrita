# Spec: Simplify Puzzle Generation

## Context & Motivation

Puzzle generation is too slow. The LLM prompt currently asks for `word`, `category`,
`difficulty`, and `5 hints` — forcing the model to reason about difficulty level and
categorize the word. This adds latency and increases validation failures.

**Simplification approach:**
- Ask the LLM only for `word` + `3 hints`
- Remove `category` and `difficulty` from the prompt entirely
- Word length range: 4–8 letters only
- Difficulty is implicit: controlled by word length, progressing across generation cycles
- Reduce hints from 5 → 3 to speed up generation
- Batch size: 20 puzzles per cycle

## Progressive Difficulty via Generation Cycles

Player level = number of completed generation cycles. No XP system needed.

Each cycle generates 20 puzzles. The **minimum word length increases** each cycle:

| Cycle | Min letters | Max letters | Lengths generated | Words per length |
|-------|-------------|-------------|-------------------|------------------|
| 0     | 4           | 8           | 4, 5, 6, 7, 8    | 4 each = 20      |
| 1     | 5           | 8           | 5, 6, 7, 8       | 5 each = 20      |
| 2     | 6           | 8           | 6, 7, 8          | ~7 each = 20     |
| 3     | 7           | 8           | 7, 8             | 10 each = 20     |
| 4+    | 8           | 8           | 8                 | 20               |

**Play order within a batch:** all 4-letter words first → all 5-letter → ... → all 8-letter.
When the batch is exhausted, ask the user if they want to generate more.

The **generation cycle counter** is stored in `AppPreferences`.

## Requirements

### Functional

- [ ] LLM prompt requests only `word` (string) and `hints` (3 strings)
- [ ] Word length is 4–8 letters; no word shorter than 4 or longer than 8
- [ ] `category` is removed from prompt, parser, and validator
- [ ] Existing `category` DB column is kept with default empty string (no Room migration)
- [ ] Domain model `Puzzle.category` keeps the field but defaults to `""`
- [ ] The `difficulty` field in DB stores the **word length** (4–8), not a difficulty tier
- [ ] Hints reduced from 5 → 3 per puzzle
- [ ] Batch generates 20 puzzles per cycle, distributed across word lengths based on cycle
- [ ] Prompt templates (large + compact) are both updated
- [ ] Parser accepts responses with only `word` + `hints` (no `category`/`difficulty`)
- [ ] Validator no longer checks for non-blank category
- [ ] Validator enforces at least 3 hints; if more are returned, take the first 3
- [ ] `PuzzleResponse` drops `category` and `difficulty` fields
- [ ] Generation cycle counter stored in `AppPreferences`
- [ ] `AppPreferences` exposes `generationCycle: Flow<Int>` (default 0)
- [ ] After a batch completes, increment the cycle counter
- [ ] Puzzles are served in ascending word-length order within a batch
- [ ] When all puzzles are played, prompt user to generate more (existing mechanic)

### Non-Functional

- [ ] Generation time should decrease noticeably (simpler prompt = fewer tokens)
- [ ] Retry rate should decrease (fewer validation rules = fewer failures)

## Acceptance Criteria

### Prompt

- [ ] Given a generation request, when the prompt is built, then it must NOT mention category or difficulty
- [ ] Given a request for 5-letter words, when the prompt is built, then it must request a word with exactly 5 letters
- [ ] Given a request for 8-letter words, when the prompt is built, then it must request a word with exactly 8 letters

### Parser

- [ ] Given a JSON response `{"word":"gato","hints":["h1","h2","h3"]}`, when parsed, then it returns a successful PuzzleResponse with word="gato" and 3 hints
- [ ] Given a JSON response with extra fields (category, difficulty), when parsed, then it still parses successfully (ignore unknown keys)
- [ ] Given a malformed response with identifiable word + hints array, when parsed structurally, then it extracts word and hints correctly

### Validator

- [ ] Given a valid word within the expected length and 3 hints, when validated, then result is Valid
- [ ] Given a word outside the expected length, when validated, then result is Invalid with reason
- [ ] Given a response with 5 hints, when validated, then it takes the first 3 and result is Valid
- [ ] Given a response with 2 hints, when validated, then result is Invalid (must have at least 3)
- [ ] Given a hint that contains the word, when validated, then result is Invalid

### Generation Cycles

- [ ] Given cycle 0, when computing word lengths, then lengths are [4, 5, 6, 7, 8] with 4 words each
- [ ] Given cycle 1, when computing word lengths, then lengths are [5, 6, 7, 8] with 5 words each
- [ ] Given cycle 2, when computing word lengths, then lengths are [6, 7, 8] with ~7 words each
- [ ] Given cycle 3, when computing word lengths, then lengths are [7, 8] with 10 words each
- [ ] Given cycle 4+, when computing word lengths, then length is [8] with 20 words
- [ ] Given cycle 7 (beyond max), when computing word lengths, then it behaves like cycle 4 (capped)
- [ ] Given a completed batch, when generation finishes, then the cycle counter increments by 1

### Domain Model

- [ ] Given a generated puzzle, when saved to DB, then `category` column is empty string
- [ ] Given a 6-letter word, when saved, then `difficulty` field is 6 (= word length)

### Play Order

- [ ] Given a batch with mixed word lengths, when serving puzzles, then 4-letter words come first, then 5, 6, 7, 8

## Edge Cases

- LLM returns a response with `category` or `difficulty` fields → parser ignores them
- LLM returns 5 hints instead of 3 → take first 3, discard the rest
- Existing puzzles in DB have 5 hints → game UI must handle both 3 and 5 hints gracefully
- Existing puzzles have `difficulty` 1–5 (old meaning) → won't conflict; old puzzles are already played
- Cycle counter persists across app restarts via AppPreferences

## Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Max word length | 8 letters | Avoids rare/hard-to-generate long words; keeps game accessible |
| Difficulty field reuse | Stores word length (4–8) | Avoids Room migration; repurposes existing column |
| Hints per puzzle | 3 (was 5) | Fewer hints = faster generation; 3 is enough for a word-guessing game |
| Batch size | 20 per cycle | Consistent across all cycles; distribution changes, total stays same |
| Category field | Keep in DB with default "" | Avoids Room migration; field is ignored but backwards-compatible |
| Cycle storage | AppPreferences | Simple, already exists, no schema change needed |
| Player level | = cycle count | No XP calculation; purely based on how many times user generated words |
| Cycle cap | Cycle 4+ stays at 8-letter words | Once mastered, player stays at hardest level |

## Out of Scope

- Changing the game UI hint display (must handle both 3 and 5 hints from existing data)
- Room database migration
- XP system or level calculation beyond cycle count
- Rebalancing replenishment threshold
- UI for showing player level (can be added later based on cycle count)
