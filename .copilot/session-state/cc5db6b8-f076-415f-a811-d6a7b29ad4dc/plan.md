# Plan: Code Maturity Improvements

## Problem

Every change introduces cascading bugs because of:
- 13 Fake implementations duplicated across 4 test files
- Magic numbers (`6`, `3`, `10`, `5`) scattered across production + test code
- SettingsViewModel orchestrating 6 dependencies directly
- 8 ghost columns in PlayerStatsEntity creating confusion
- `ChatRepository.getPuzzle()` crossing aggregate boundaries

Since the app is NOT in production, we can make breaking changes freely.

## Approach

5 changes, ordered by impact:

### 1. Create `core/testing` module with shared fakes

Create `core/testing` that depends on `core/model` + `core/ai` (for LlmEngineManager/PuzzleGenerationScheduler interfaces). Feature modules add `testImplementation(project(":core:testing"))` and delete their private inner fakes.

**Fakes to create (one canonical each):**
- `FakeStatsRepository` (replaces 3 copies)
- `FakePuzzleRepository` (replaces 2 copies)
- `FakeGameSessionRepository` (replaces 2 copies)
- `FakeChatRepository` (replaces 2 copies)
- `FakeModelRepository` (replaces 2 copies)
- `FakeLlmEngineManager` (replaces 2 copies)
- `FakeGenerationScheduler` (replaces 1 copy)
- `FakeAppPreferences` (replaces 1 copy)
- `FakeModelDownloadManager` (replaces 1 copy)

### 2. Create `GameRules` object in `core/model`

Centralize all game rule constants, then replace hardcoded literals everywhere:
- MAX_ATTEMPTS = 6 (GameViewModel:122, GameLogic:60, GameScreen:210, tests)
- MIN_HINTS = 3 (PuzzleValidator:33)
- MAX_CHAT_MESSAGES = 10 (ChatState:14, ChatViewModelTest:237)
- MAX_WORD_LENGTH = 6 (PuzzleGenerationWorker:170)
- GENERATION_LEVELS (PuzzleGenerationWorker:176)
- REPLENISHMENT_THRESHOLD = 5 (PuzzleGenerationWorker:169)

### 3. Drop legacy columns from PlayerStatsEntity

Bump Room version 3→4 with `fallbackToDestructiveMigration()`. Remove 8 columns: currentDifficulty, maxUnlockedDifficulty, totalXp, playerTier, gamesWonByDifficulty, winRateByDifficulty, consecutiveLossesAtCurrent, wordSizePreference. Clean mapper and TestFixtures.

### 4. Extract `ResetProgressUseCase`

Move 4-repo reset from SettingsViewModel into `core/model/usecase/ResetProgressUseCase`. SettingsViewModel injects just the use case instead of 4 repos.

### 5. Move `getPuzzle()` out of ChatRepository

ChatViewModel should use PuzzleRepository directly. Remove `getPuzzle()` from ChatRepository interface and implementation.
