# Puzzle Generation

This document maps the complete puzzle generation flow in Palabrita — from trigger conditions to database persistence — including all states, key functions, and the connections between components.

## Overview

Palabrita uses a simplified 3-level, single-word-length-per-generation system ([Spec 15](../specs/15-simplified-level-generation.md)):

| Cycle | Word Length | Batch Size | Purpose |
|-------|-------------|------------|---------|
| 0 | 4 letters | 5 words | Fast onboarding |
| 1 | 5 letters | 10 words | Regular play |
| ≥2 | 6 letters | 10 words | Ongoing (capped) |

Generation always runs **synchronously** — the user stays on `GenerationScreen` and waits for it to complete. There is no background generation via WorkManager.

---

## Files & Responsibilities

### `core/ai/`

| File | Role | Key Classes |
|------|------|-------------|
| `GeneratePuzzlesUseCase.kt` | Use case that orchestrates the full generation + persistence flow | `GeneratePuzzlesUseCase` (interface), `GeneratePuzzlesUseCaseImpl`, `GenerationResult` |
| `PuzzleGenerator.kt` | LLM generation loop (batch + single-puzzle retry) | `PuzzleGenerator`, `PuzzleGeneratorImpl` |
| `LlmEngineManager.kt` | LLM engine lifecycle interface | `LlmEngineManager`, `LlmSession`, `EngineState` |
| `LlmEngineManagerImpl.kt` | LiteRT-LM engine initialization (GPU → CPU fallback) | `LlmEngineManagerImpl`, `LlmSessionImpl` |
| `GenerationActivity.kt` | Per-puzzle live activity enum | `GenerationActivity` |
| `LlmResponseParser.kt` | Parses JSON from LLM responses | `LlmResponseParser`, `PuzzleResponse`, `ParseResult` |
| `PuzzleValidator.kt` | Validates puzzle (word length, hints, duplicates) | `PuzzleValidator`, `ValidationResult` |
| `PromptTemplates.kt` | LLM prompt constants | `PromptTemplates` |

### `core/model/`

| File | Role |
|------|------|
| `GameRules.kt` | Constants + `levelForCycle(cycle)` — the cycle-to-difficulty mapping |
| `Puzzle.kt` | Domain model (word, wordDisplay, hints, difficulty, source, language) |
| `PuzzleSource.kt` | `STATIC` or `AI` enum |
| `repository/PuzzleRepository.kt` | Repository interface |
| `preferences/AppPreferences.kt` | `generationCycle: Flow<Int>`, `incrementGenerationCycle()` |

### `core/data/`

| File | Role |
|------|------|
| `repository/PuzzleRepositoryImpl.kt` | Room-backed repository |
| `db/dao/PuzzleDao.kt` | Queries: `getNextUnplayed`, `countAllUnplayed`, `insertAll`, `markAsPlayed`, `getAllWords`, `getRecentWords` |
| `db/entity/PuzzleEntity.kt` | Room entity (id, word, wordDisplay, hints, difficulty, language, isPlayed, …) |

### `core/testing/`

| File | Role |
|------|------|
| `FakeGeneratePuzzlesUseCase.kt` | Fake for unit tests — configure `result` to control the outcome |
| `FakePuzzleGenerator.kt` | Fake that returns empty list by default |
| `FakeLlmEngineManager.kt` | Fake with controllable `EngineState`; call `setError()` to simulate failure |

### `feature/onboarding/`

| File | Role |
|------|------|
| `GenerationViewModel.kt` | Calls `GeneratePuzzlesUseCase.execute()` directly; owns `GenerationState` |
| `GenerationScreen.kt` | Step-by-step generation UI (Composable) |
| `OnboardingViewModel.kt` | State machine: Welcome → Language → ModelSelection → Download → Generation → Complete |

---

## State Definitions

### `EngineState` — `LlmEngineManager`

```
Uninitialized
    └─► Initializing
            ├─► Ready
            └─► Error(message)
```

### `GenerationActivity` — `PuzzleGenerator` (per-puzzle, real-time)

```
CREATING → VALIDATING → ACCEPTED
                     └─► VALIDATION_FAILED  (will retry)
                     └─► FAILED_RETRYING    (all retries exhausted)
```

### `GenerationState` — `GenerationViewModel` (UI)

```kotlin
data class GenerationState(
    val isGenerating: Boolean = true,
    val isComplete: Boolean = false,
    val failed: Boolean = false,
    val cancelled: Boolean = false,
    val progress: GenerationProgress = GenerationProgress(),
    val steps: List<GenerationStep> = emptyList(),
    val currentActivityResId: Int? = null,
)
```

### `GenerationResult` — `GeneratePuzzlesUseCase`

```kotlin
data class GenerationResult(
    val generatedCount: Int,
    val batchSize: Int,   // -1 means generation was skipped (enough puzzles already existed)
)
```

---

## Complete Flow

### 1. Onboarding Trigger

```
Model download completes
    └─► OnboardingViewModel.initializeEngineAndGenerate(modelPath)
            └─► engineManager.initialize(modelPath)   [Dispatchers.IO]
                    ├─► engineState = Initializing
                    ├─► initializeWithFallback()
                    │       └─► Backend.GPU() with fallback to Backend.CPU()
                    └─► engineState = Ready | Error

engineState = Ready
    └─► OnboardingScreen navigates to GenerationRoute
            └─► GenerationScreen composed
                    └─► LaunchedEffect { viewModel.triggerGeneration(modelId) }
```

### 2. `GenerationViewModel.triggerGeneration()`

```
triggerGeneration(modelId)
    ├─► Resolve modelId (or load from modelRepository.getConfig())
    ├─► If ModelId.NONE → failed = true, return
    ├─► Reset state to GenerationState()
    ├─► If !engineManager.isReady() → engineManager.initialize(modelPath)
    ├─► If engineState is Error → failed = true, return
    ├─► language = statsRepository.getStats().preferredLanguage
    └─► result = generatePuzzlesUseCase.execute(language, modelId) { successCount, batchSize →
                    progress = GenerationProgress(successCount, batchSize)
                    steps updated in real-time
                }

    On result:
        batchSize == -1  → isComplete = true  (skipped, already had enough puzzles)
        generatedCount == 0 → failed = true
        else             → isComplete = true, progress = final counts
```

### 3. `GeneratePuzzlesUseCase.execute()`

```
execute(language, modelId, onProgress)
    ├─► unplayed = puzzleRepository.countAllUnplayed(language)
    ├─► If unplayed >= REPLENISHMENT_THRESHOLD (5)
    │       └─► return GenerationResult(0, batchSize = -1)   [skip]
    ├─► Require engineManager.isReady()
    ├─► existingWords = puzzleRepository.getAllGeneratedWords()
    ├─► recentWords  = puzzleRepository.getRecentWords(50)
    ├─► cycle = appPreferences.generationCycle.first()
    ├─► (wordLength, batchSize) = GameRules.levelForCycle(cycle)
    ├─► puzzles = puzzleGenerator.generateBatch(batchSize, language, wordLength, ...) { successCount →
    │       onProgress(successCount, batchSize)
    │   }
    ├─► puzzleRepository.savePuzzles(puzzles)
    ├─► If missing > 0 (partial batch):
    │       └─► Retry once with updated existingWords
    │               └─► puzzleRepository.savePuzzles(retryPuzzles)
    ├─► If generatedCount > 0:
    │       └─► appPreferences.incrementGenerationCycle()
    └─► return GenerationResult(generatedCount, batchSize)
```

### 4. `PuzzleGenerator.generateBatch()`

```
generateBatch(count, language, wordLength, recentWords, allExistingWords, modelId, onProgress)
    ├─► Require engineManager.isReady()
    ├─► usedWords = allExistingWords.toMutableSet()
    ├─► While puzzleIndex < count:
    │       ├─► chunkSize = min(remaining, SESSION_ROTATION = 5)
    │       └─► engineManager.createChatSession(systemPrompt).use { session →
    │               └─► repeat(chunkSize):
    │                       └─► generateSinglePuzzle(session, ..., usedWords)
    │                               ├─► success → add to generated, usedWords, call onProgress
    │                               └─► null    → slot lost (5 retries exhausted)
    │           }
    └─► return generated
```

### 5. `PuzzleGenerator.generateSinglePuzzle()`

```
generateSinglePuzzle(session, language, wordLength, recentWords, usedWords, modelId): Puzzle?
    └─► repeat(MAX_GENERATION_RETRIES = 5):
            ├─► _activity = CREATING
            ├─► response = session.sendMessage(userPrompt)     [LLM call]
            ├─► _activity = VALIDATING
            ├─► parseResult = parser.parsePuzzle(response)
            ├─► If parse failed → retry
            ├─► validationResult = validator.validate(puzzle, usedWords, wordLength..wordLength)
            ├─► If invalid → _activity = VALIDATION_FAILED → retry
            └─► If valid:
                    ├─► _activity = ACCEPTED
                    └─► return Puzzle(
                              word = TextNormalizer.normalizeToAscii(response.word),
                              wordDisplay = response.word,
                              hints = response.hints.take(3),
                              difficulty = wordLength,
                              source = PuzzleSource.AI
                            )
        _activity = FAILED_RETRYING
        return null
```

### 6. Puzzle Serving (Game)

```
GameViewModel.selectNextPuzzle(language)
    └─► puzzleRepository.getNextUnplayed(language)
            └─► SQL: SELECT * FROM puzzles
                     WHERE isPlayed = 0 AND language = :lang
                     ORDER BY difficulty ASC, RANDOM()
                     LIMIT 1

[After answer submitted]
puzzleRepository.markAsPlayed(puzzleId)
    └─► SQL: UPDATE puzzles SET isPlayed = 1, playedAt = :now WHERE id = :id
```

---

## Trigger Conditions

| Trigger | Entry Point | Context |
|---------|-------------|---------|
| Onboarding (post-download) | `GenerationScreen` via `OnboardingViewModel` | First run after model is ready |
| Home screen — no puzzles left | `GenerationScreen` via `HomeViewModel` | Unplayed count hits 0 |
| Settings — manual regenerate | `GenerationScreen(isRegeneration=true)` | User-initiated in Settings |
| Language change | `SettingsViewModel` | Unplayed count < 5 in new language |

---

## Key Constants (`core/model/GameRules.kt`)

```kotlin
const val MAX_ATTEMPTS             = 6    // Attempts per puzzle in game
const val MIN_HINTS                = 3    // Hints per puzzle
const val MAX_WORD_LENGTH          = 6    // Maximum letter count
const val REPLENISHMENT_THRESHOLD  = 5    // Skip generation when unplayed >= 5
const val MAX_GENERATION_RETRIES   = 5    // Max retries per puzzle slot
const val SESSION_ROTATION         = 5    // New LLM session every N puzzles

val GENERATION_LEVELS = listOf(
    4 to 5,   // Cycle 0
    5 to 10,  // Cycle 1
    6 to 10   // Cycle 2+
)
```

---

## Partial Batch Retry

If the LLM fails some slots (e.g., generates 7 of 10), the use case does one retry pass:

1. Calculate `missing = batchSize - generatedCount`
2. Call `generateBatch(missing, ...)` with the **updated** `existingWords` (includes already-generated words to avoid intra-batch duplicates)
3. Save both batches to the database
4. `incrementGenerationCycle()` only if `generatedCount > 0`

---

## Component Dependency Map

```
GenerationScreen
    └─► GenerationViewModel
            ├─► LlmEngineManager          (engine init)
            ├─► GeneratePuzzlesUseCase    (orchestrates full generation)
            │       ├─► PuzzleGenerator
            │       │       ├─► LlmEngineManager.createChatSession
            │       │       ├─► LlmResponseParser
            │       │       └─► PuzzleValidator
            │       ├─► PuzzleRepository
            │       │       └─► PuzzleDao (Room)
            │       └─► AppPreferences    (generationCycle)
            ├─► StatsRepository           (preferred language)
            └─► PuzzleGenerator.activity  (real-time per-puzzle feedback)
```

This document maps the complete puzzle generation flow in Palabrita — from trigger conditions to database persistence — including all states, key functions, and the connections between components.

## Overview

Palabrita uses a simplified 3-level, single-word-length-per-generation system ([Spec 15](../specs/15-simplified-level-generation.md)):

| Cycle | Word Length | Batch Size | Purpose |
|-------|-------------|------------|---------|
| 0 | 4 letters | 5 words | Fast onboarding |
| 1 | 5 letters | 10 words | Regular play |
| ≥2 | 6 letters | 10 words | Ongoing (capped) |

Generation is triggered both **synchronously during onboarding** and **asynchronously via WorkManager** when the unplayed puzzle count falls below the replenishment threshold (5).

---

## Files & Responsibilities

### `core/ai/`

| File | Role | Key Classes |
|------|------|-------------|
| `PuzzleGenerator.kt` | Main generation interface & implementation | `PuzzleGenerator`, `PuzzleGeneratorImpl` |
| `LlmEngineManager.kt` | LLM engine lifecycle interface | `LlmEngineManager`, `LlmSession`, `EngineState` |
| `LlmEngineManagerImpl.kt` | LiteRT-LM engine initialization (GPU → CPU fallback) | `LlmEngineManagerImpl`, `LlmSessionImpl` |
| `worker/PuzzleGenerationWorker.kt` | WorkManager `CoroutineWorker` that runs the generation | `PuzzleGenerationWorker` |
| `worker/PuzzleGenerationScheduler.kt` | WorkManager scheduling interface & state types | `PuzzleGenerationScheduler`, `GenerationWorkState`, `GenerationInfo`, `GenerationProgress` |
| `worker/PuzzleGenerationSchedulerImpl.kt` | Schedules unique work, exposes state as Flow | `PuzzleGenerationSchedulerImpl` |
| `GenerationActivity.kt` | Per-puzzle live activity enum | `GenerationActivity` |
| `LlmResponseParser.kt` | Parses JSON from LLM responses | `LlmResponseParser`, `PuzzleResponse`, `ParseResult` |
| `PuzzleValidator.kt` | Validates puzzle (word length, hints, duplicates) | `PuzzleValidator`, `ValidationResult` |
| `PromptTemplates.kt` | LLM prompt constants | `PromptTemplates` |

### `core/model/`

| File | Role |
|------|------|
| `GameRules.kt` | Constants + `levelForCycle(cycle)` — the cycle-to-difficulty mapping |
| `Puzzle.kt` | Domain model (word, wordDisplay, hints, difficulty, source, language) |
| `PuzzleSource.kt` | `STATIC` or `AI` enum |
| `repository/PuzzleRepository.kt` | Repository interface |
| `preferences/AppPreferences.kt` | `generationCycle: Flow<Int>`, `incrementGenerationCycle()` |

### `core/data/`

| File | Role |
|------|------|
| `repository/PuzzleRepositoryImpl.kt` | Room-backed repository |
| `db/dao/PuzzleDao.kt` | Queries: `getNextUnplayed`, `countAllUnplayed`, `insertAll`, `markAsPlayed`, `getAllWords`, `getRecentWords` |
| `db/entity/PuzzleEntity.kt` | Room entity (id, word, wordDisplay, hints, difficulty, language, isPlayed, …) |

### `feature/onboarding/`

| File | Role |
|------|------|
| `GenerationViewModel.kt` | Orchestrates engine init + generation scheduling + UI state |
| `GenerationScreen.kt` | Step-by-step generation UI (Composable) |
| `OnboardingViewModel.kt` | State machine: Welcome → Language → ModelSelection → Download → Generation → Complete |

---

## State Definitions

### `EngineState` — `LlmEngineManager`

```
Uninitialized
    └─► Initializing
            ├─► Ready
            └─► Error(message)
```

### `GenerationWorkState` — `PuzzleGenerationScheduler`

```
IDLE → RUNNING → SUCCEEDED
              └─► FAILED
```

### `GenerationActivity` — `PuzzleGenerator` (per-puzzle, real-time)

```
CREATING → VALIDATING → ACCEPTED
                     └─► VALIDATION_FAILED  (will retry)
                     └─► FAILED_RETRYING    (all retries exhausted)
```

### `GenerationState` — `GenerationViewModel` (UI)

```kotlin
data class GenerationState(
    val isGenerating: Boolean = true,
    val isComplete: Boolean = false,
    val failed: Boolean = false,
    val cancelled: Boolean = false,
    val progress: GenerationProgress = GenerationProgress(),
    val steps: List<GenerationStep> = emptyList(),
    val currentActivityResId: Int? = null,
)
```

---

## Complete Flow

### 1. Onboarding Trigger

```
Model download completes
    └─► OnboardingViewModel.initializeEngineAndGenerate(modelPath)
            └─► engineManager.initialize(modelPath)   [Dispatchers.IO]
                    ├─► engineState = Initializing
                    ├─► initializeWithFallback()
                    │       └─► Backend.GPU() with fallback to Backend.CPU()
                    └─► engineState = Ready | Error

engineState = Ready
    └─► OnboardingScreen navigates to GenerationRoute
            └─► GenerationScreen composed
                    └─► LaunchedEffect { viewModel.triggerGeneration(modelId) }
```

### 2. `GenerationViewModel.triggerGeneration()`

```
triggerGeneration(modelId)
    ├─► hasTriggered = true
    ├─► Resolve modelId (or load from modelRepository.getConfig())
    ├─► If ModelId.NONE → mark failed, return
    ├─► If !engineManager.isReady() → engineManager.initialize(modelPath)
    └─► expectedWorkId = generationScheduler.scheduleGeneration(modelId)

observeGeneration() — combines engineState + generationInfo:
    RUNNING   → hasSeenRunning = true; isGenerating = true
    SUCCEEDED → guard (hasTriggered && hasSeenRunning && workId matches)
                  totalExpected == -1  → skip (enough puzzles existed)
                  generatedCount == 0  → failed = true
                  else                 → isComplete = true
    FAILED    → guard (hasTriggered && hasSeenRunning) → failed = true

observeActivity() — collects puzzleGenerator.activity:
    └─► currentActivityResId = activityToResId(activity)
```

### 3. WorkManager Execution — `PuzzleGenerationWorker.doWork()`

```
doWork()
    ├─► setForeground(createForegroundInfo())
    ├─► language = statsRepository.getStats().preferredLanguage
    ├─► unplayed = puzzleRepository.countAllUnplayed(language)
    ├─► If unplayed >= REPLENISHMENT_THRESHOLD (5)
    │       └─► return Result.success(totalExpected = -1)   [skip]
    ├─► If !engineManager.isReady() → return Result.retry()
    ├─► existingWords = puzzleRepository.getAllGeneratedWords()
    ├─► recentWords  = puzzleRepository.getRecentWords(50)
    ├─► cycle = appPreferences.generationCycle.first()
    ├─► (wordLength, batchSize) = GameRules.levelForCycle(cycle)
    ├─► setProgress(0, batchSize)
    ├─► puzzles = puzzleGenerator.generateBatch(batchSize, language, wordLength, ...)
    ├─► puzzleRepository.savePuzzles(puzzles)
    ├─► If missing > 0 (partial batch):
    │       └─► Retry once with updated existingWords
    │               └─► puzzleRepository.savePuzzles(retryPuzzles)
    ├─► If generatedCount > 0:
    │       ├─► appPreferences.incrementGenerationCycle()
    │       └─► showCompletionNotification(generatedCount)
    └─► return Result.success(generatedCount, batchSize)
```

### 4. `PuzzleGenerator.generateBatch()`

```
generateBatch(count, language, wordLength, recentWords, allExistingWords, modelId)
    ├─► Require engineManager.isReady()
    ├─► usedWords = allExistingWords.toMutableSet()
    ├─► While puzzleIndex < count:
    │       ├─► chunkSize = min(remaining, SESSION_ROTATION = 5)
    │       └─► engineManager.createChatSession(systemPrompt).use { session →
    │               └─► repeat(chunkSize):
    │                       └─► generateSinglePuzzle(session, ..., usedWords)
    │                               ├─► success → add to generated, usedWords, puzzleIndex++
    │                               └─► null    → puzzleIndex++ (slot lost)
    │           }
    └─► return generated
```

### 5. `PuzzleGenerator.generateSinglePuzzle()`

```
generateSinglePuzzle(session, language, wordLength, recentWords, usedWords, modelId): Puzzle?
    └─► repeat(MAX_GENERATION_RETRIES = 5):
            ├─► _activity = CREATING
            ├─► response = session.sendMessage(userPrompt)     [LLM call]
            ├─► _activity = VALIDATING
            ├─► parseResult = parser.parsePuzzle(response)
            ├─► If parse failed → retry
            ├─► validationResult = validator.validate(puzzle, usedWords, wordLength..wordLength)
            ├─► If invalid → _activity = VALIDATION_FAILED → retry
            └─► If valid:
                    ├─► _activity = ACCEPTED
                    └─► return Puzzle(
                              word = TextNormalizer.normalizeToAscii(response.word),
                              wordDisplay = response.word,
                              hints = response.hints.take(3),
                              difficulty = wordLength,
                              source = PuzzleSource.AI
                            )
        _activity = FAILED_RETRYING
        return null
```

### 6. Puzzle Serving (Game)

```
GameViewModel.selectNextPuzzle(language)
    └─► puzzleRepository.getNextUnplayed(language)
            └─► SQL: SELECT * FROM puzzles
                     WHERE isPlayed = 0 AND language = :lang
                     ORDER BY difficulty ASC, RANDOM()
                     LIMIT 1

[After answer submitted]
puzzleRepository.markAsPlayed(puzzleId)
    └─► SQL: UPDATE puzzles SET isPlayed = 1, playedAt = :now WHERE id = :id
```

---

## Trigger Conditions

| Trigger | Entry Point | Context |
|---------|-------------|---------|
| Onboarding (post-download) | `GenerationScreen` via `OnboardingViewModel` | First run after model is ready |
| Home screen — no puzzles left | `GenerationScreen` via `HomeViewModel` | Unplayed count hits 0 |
| Settings — manual regenerate | `GenerationScreen(isRegeneration=true)` | User-initiated in Settings |
| Language change | `SettingsViewModel` | Unplayed count < 5 in new language |
| Background replenishment | `PuzzleGenerationWorker` | Unplayed < REPLENISHMENT_THRESHOLD (5) |

---

## Key Constants (`core/model/GameRules.kt`)

```kotlin
const val MAX_ATTEMPTS             = 6    // Attempts per puzzle in game
const val MIN_HINTS                = 3    // Hints per puzzle
const val MAX_WORD_LENGTH          = 6    // Maximum letter count
const val REPLENISHMENT_THRESHOLD  = 5    // Generate when unplayed < 5
const val MAX_GENERATION_RETRIES   = 5    // Max retries per puzzle slot
const val SESSION_ROTATION         = 5    // New LLM session every N puzzles

val GENERATION_LEVELS = listOf(
    4 to 5,   // Cycle 0
    5 to 10,  // Cycle 1
    6 to 10   // Cycle 2+
)
```

---

## Race Condition: Stale SUCCEEDED State

When `GenerationViewModel` starts observing WorkManager, a `SUCCEEDED` state from a **previous** generation run might already be present. Naively consuming it would set `isComplete = true` before the current generation even starts.

**Fix — two guard flags:**

| Flag | Set to `true` when |
|------|--------------------|
| `hasTriggered` | `triggerGeneration()` is called |
| `hasSeenRunning` | `RUNNING` state is observed for the current work |

`isComplete = true` is only set when **both flags are true** and the `workId` matches the one returned by `scheduleGeneration()`.

```kotlin
GenerationWorkState.SUCCEEDED -> {
    if (!hasTriggered || !hasSeenRunning) return@collect   // ignore stale
    if (info.workId != null && info.workId != expectedWorkId) return@collect
    // safe to mark complete
}
```

---

## Partial Batch Retry

If the LLM fails some slots (e.g., generates 7 of 10), the worker does one retry pass:

1. Calculate `missing = batchSize - generatedCount`
2. Call `generateBatch(missing, ...)` with the **updated** `existingWords` (includes already-generated words to avoid intra-batch duplicates)
3. Save both batches to the database
4. `incrementGenerationCycle()` only if `generatedCount > 0`

---

## Component Dependency Map

```
GenerationScreen
    └─► GenerationViewModel
            ├─► LlmEngineManager          (engine init + session creation)
            ├─► PuzzleGenerationScheduler (schedule + observe work state)
            └─► PuzzleGenerator.activity  (real-time per-puzzle feedback)

PuzzleGenerationWorker
    ├─► LlmEngineManager
    ├─► PuzzleGenerator
    │       ├─► LlmEngineManager.createChatSession
    │       ├─► LlmResponseParser
    │       └─► PuzzleValidator
    ├─► PuzzleRepository
    │       └─► PuzzleDao (Room)
    ├─► StatsRepository    (preferred language)
    └─► AppPreferences     (generationCycle)
```
