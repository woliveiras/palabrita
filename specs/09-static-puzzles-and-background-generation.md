# Spec: Static Puzzles & Background Generation

## Context & Motivation

The current onboarding flow blocks the user with a "Generating puzzles…" screen while the LLM creates 7 puzzles synchronously. This has several problems:

1. **Slow start**: The user waits ~1-3 minutes before playing their first game
2. **Misleading progress**: The UI shows "Puzzle 0 of 7…" but the batch call returns all at once, so progress is stuck at 0 then jumps to 7
3. **Too few puzzles**: 7 puzzles are consumed quickly; the user then has nothing to play
4. **Difficulty mismatch**: Onboarding generates only difficulty 1, but `getNextUnplayed()` filters by exact difficulty — choosing difficulty 2+ returns no puzzle
5. **Light mode is empty**: Users with `ModelId.NONE` skip generation but have zero puzzles

## Requirements

### Functional

- [ ] Ship a static puzzle dataset embedded in the app (JSON asset)
- [ ] Provide ~50 puzzles per supported language (pt-br, en, es) across all 5 difficulty levels
- [ ] Seed the database with static puzzles on first launch (before or during onboarding)
- [ ] Remove the "Generation" step from the onboarding flow — user goes straight to the game after model download (or after language selection in Light mode)
- [ ] Start AI puzzle generation in background via WorkManager after engine initialization
- [ ] Background generation targets 30 puzzles per language, spread across difficulty levels
- [ ] Background generation only runs when unplayed puzzle count drops below a threshold (e.g., < 10)
- [ ] Static and AI-generated puzzles coexist in the same `puzzles` table, distinguished by `source` (STATIC vs AI)
- [ ] User plays from a unified queue: AI puzzles are preferred when available, static as fallback

### Non-Functional

- [ ] Static puzzle JSON must be < 200 KB per language
- [ ] Database seeding must complete in < 2 seconds
- [ ] Background generation must not impact gameplay performance (use `Dispatchers.IO`, low-priority WorkManager constraints)
- [ ] WorkManager job must be idempotent (safe to re-run)

## Acceptance Criteria

### Static Puzzle Seeding

- [ ] Given a fresh install, when the app starts, then the database contains at least 50 puzzles for the default language
- [ ] Given a fresh install, when the user selects language "en" in onboarding, then static puzzles for "en" are available
- [ ] Given static puzzles are seeded, when the user picks any difficulty (1-5), then `getNextUnplayed()` returns a puzzle
- [ ] Given the database already has static puzzles, when the app starts again, then no duplicate seeding occurs

### Onboarding Flow

- [ ] Given the user is in AI mode, when model download completes, then onboarding skips the generation step and navigates to game
- [ ] Given the user is in Light mode, when language is selected, then onboarding completes without generation step
- [ ] Given the user completes onboarding, when they reach the difficulty screen, then puzzles are available for all difficulties

### Background Generation

- [ ] Given the AI engine is ready, when onboarding completes, then a WorkManager job is enqueued to generate puzzles
- [ ] Given unplayed puzzles < 10 for the current language, when the generation worker runs, then it generates up to 30 puzzles
- [ ] Given unplayed puzzles >= 10, when the generation worker is triggered, then it skips generation
- [ ] Given the device is on low battery, when the worker is scheduled, then it respects battery constraints (defers execution)
- [ ] Given generation is in progress, when the user starts a game, then gameplay is unaffected (no UI blocking)
- [ ] Given background generation completes successfully, when puzzles are saved, then a push notification is shown telling the user new AI-generated challenges are ready

### Puzzle Queue

- [ ] Given both AI and static puzzles exist for a difficulty, when the user starts a game, then AI puzzles are served first
- [ ] Given no AI puzzles exist but static puzzles do, when the user starts a game, then a static puzzle is served
- [ ] Given no puzzles exist for the chosen difficulty, when the user starts a game, then an error message is shown

## Edge Cases

- What if the static JSON is corrupted? → Fail gracefully, log error, onboarding continues (user waits for AI generation or gets error)
- What if WorkManager job fails mid-generation? → Partially generated puzzles are kept; job retries with backoff
- What if user switches language? → Seed static puzzles for new language if not already present; trigger background generation for new language
- What if device runs out of storage during generation? → Worker catches exception, retries later
- What if all static puzzles for a difficulty are played? → AI puzzles fill the gap; if none available, show "no puzzle" error

## Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Puzzle count per language (static) | ~50 (10 per difficulty) | Covers ~5 sessions per difficulty before needing AI puzzles |
| Background generation target | 30 per language | Balances generation time (~15-30 min) with content depth |
| Replenishment threshold | < 10 unplayed | Triggers generation before user runs out |
| Puzzle priority | AI > Static | AI puzzles are more varied and surprising |
| Static data format | JSON in assets/ | Simple, no network dependency, easy to update |
| Generation step in onboarding | Removed entirely | Static puzzles make it unnecessary |
| WorkManager constraints | Network not required, battery not low | Generation is CPU-bound and local |
| Completion notification | Push notification via NotificationManager | User knows AI puzzles are ready for a better experience |

## Out of Scope

- Server-side puzzle generation or cloud sync
- User-created puzzles
- Difficulty-adaptive generation (always generates spread across all levels)
- Puzzle expiration or rotation
- Analytics on puzzle consumption rate
