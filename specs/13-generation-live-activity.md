# Spec: Generation Screen — Live Activity Feed

## Context & Motivation

The generation screen shows a puzzle counter that increments, but the user has no visibility into
what is actually happening at each step. Internally, the model creates puzzles, which go through
parsing and validation — and can fail and retry multiple times. Surfacing these events turns a
silent wait into an informative experience that builds trust in the AI process.

## Requirements

### Functional

- [x] `PuzzleGenerator` exposes a `StateFlow<GenerationActivity?>` that emits the current activity
      during batch generation
- [x] `GenerationActivity` is a sealed class (or enum) in `core/ai` covering at least:
  - `CREATING` — model is generating a puzzle
  - `VALIDATING` — puzzle was parsed, now being validated
  - `VALIDATION_FAILED` — validation rejected the puzzle
  - `FAILED_RETRYING` — all retries exhausted for this puzzle slot; moving on
  - `ACCEPTED` — puzzle passed validation and was saved
- [x] `GenerationViewModel` collects the flow and maps each activity to a `@StringRes` label
- [x] `GenerationState` exposes `currentActivityResId: Int?` (null when idle/complete)
- [x] `GenerationScreen` renders the current activity label below the puzzle counter card using
      `AnimatedContent` so messages transition smoothly
- [x] The activity feed is hidden when generation is complete or failed
- [x] String resources exist for all activity types in PT, EN, and ES

### Non-Functional

- [ ] Accessibility: activity label has `LiveRegion` semantics so screen readers announce changes
- [x] The `StateFlow` must reset to `null` after the batch finishes

## Acceptance Criteria

- [x] Given the model is generating, when `PuzzleGeneratorImpl` calls `sendMessage`, then
      `activity` emits `GenerationActivity.CREATING`
- [x] Given a raw LLM response was received, when `parser.parsePuzzle` is called, then `activity`
      emits `GenerationActivity.VALIDATING`
- [x] Given validation rejected a puzzle, when attempts remain, then `activity` emits
      `GenerationActivity.VALIDATION_FAILED`
- [x] Given all retries are exhausted for a slot, then `activity` emits
      `GenerationActivity.FAILED_RETRYING`
- [x] Given a puzzle passed validation, then `activity` emits `GenerationActivity.ACCEPTED`
- [x] Given `GenerationViewModel` receives `CREATING`, then `currentActivityResId` maps to
      `generation_activity_creating`
- [x] Given `GenerationViewModel` receives `ACCEPTED`, then `currentActivityResId` maps to
      `generation_activity_accepted`
- [x] Given generation is complete (`isComplete = true`), then `currentActivityResId` is `null`
- [x] Given generation has failed (`failed = true`), then `currentActivityResId` is `null`

## Edge Cases

- If the Worker is cancelled mid-generation, the `StateFlow` should emit `null` (reset) — no
  stale activity message left on screen
- Rapid successive emissions (creating → validating → failed → retrying within < 100 ms) should
  not crash the UI; `AnimatedContent` handles this gracefully
- If the screen is in the background, the ViewModel still collects but the UI simply reflects the
  latest value on resume

## Decisions

| Decision | Choice | Reasoning |
|---|---|---|
| Transport layer | `StateFlow` on `@Singleton PuzzleGenerator` | Worker and ViewModel share the same Hilt singleton — no WorkManager protocol changes needed |
| Activity model location | `core/ai` module | `PuzzleGenerator` lives there; keeps the model co-located with emission |
| String resolution layer | `GenerationViewModel` maps enum → `@StringRes` | Keeps `core/ai` free of Android resource dependencies; follows existing `GenerationStep` pattern |
| UI placement | Below the puzzle counter card | Contextually tied to the counter, visible without scrolling |
| Transition | `AnimatedContent` fade | Matches the download screen curiosity slider pattern already in the codebase |

## Out of Scope

- Showing a full scrollable log / history of past activity messages
- Per-attempt detail (e.g., showing the exact validation failure reason to the user)
- Surfacing activity during background regeneration (only relevant on the generation screen)
