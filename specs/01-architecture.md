# Spec 01 — Architecture

## Summary

Palabrita is a native Android app (Kotlin + Jetpack Compose) with a multi-module Gradle architecture. The app runs a local LLM via LiteRT-LM to generate word puzzles and offer post-guess educational chat. Devices with low RAM receive a Light mode with a static dataset.

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (type-safe routes) |
| DI | Hilt |
| Persistence | Room + DataStore (preferences) |
| LLM Runtime | LiteRT-LM Android (`com.google.ai.edge.litertlm:litertlm-android`) |
| Background | WorkManager |
| Serialization | Kotlin Serialization (JSON) |
| Build | Gradle Kotlin DSL + Version Catalog |
| Min SDK | Android 12 (API 31) |
| Target SDK | Latest stable |
| Language | Kotlin 2.x |

## Module Structure

```
palabrita/
├── app/                        → Entry point, navigation, DI root
├── core/
│   ├── model/                  → Data classes, enums, repository interfaces, GameRules
│   ├── data/                   → Room DB (v4), DAOs, repository implementations, static dataset
│   ├── ai/                     → LiteRT-LM wrapper, prompts, parser, validator
│   ├── common/                 → Device capabilities, storage checker, TextNormalizer, StateMachine, extensions
│   └── testing/                → Shared test fakes (test-only dependency)
├── feature/
│   ├── onboarding/             → Onboarding, model selection, download
│   ├── game/                   → Game screen (Wordle-style), accent keyboard
│   ├── chat/                   → Post-guess chat
│   └── settings/               → Settings, model switching, statistics
└── gradle/
    └── libs.versions.toml      → Version catalog
```

## Dependency Graph

```
app ──→ feature/onboarding
   ──→ feature/game
   ──→ feature/chat
   ──→ feature/settings

feature/onboarding ──→ core/ai, core/data, core/model, core/common
feature/game       ──→ core/data, core/model, core/common
feature/chat       ──→ core/ai, core/data, core/model
feature/settings   ──→ core/ai, core/data, core/model, core/common

core/data ──→ core/model
core/ai   ──→ core/model, core/common
core/common ──→ (no internal dependencies)
core/model  ──→ (no internal dependencies)
core/testing ──→ core/model, core/ai (testImplementation only)
```

**Rule**: no `feature/*` module depends on another `feature/*`.

## Module Responsibilities

### `app`
- `MainActivity` (single activity)
- `PalabritaApp` (Application class, Hilt entry point)
- `NavGraph` (routes: Onboarding → Game → Chat → Settings)
- App-level Hilt modules

### `core/model`
- Pure data classes: `Puzzle`, `PlayerStats`, `GameSession`, `ChatMessage`, `ModelConfig`, `DeviceTier`
- Repository interfaces: `PuzzleRepository`, `StatsRepository`, `ModelRepository`
- Enums: `PuzzleSource (AI, STATIC)`, `ModelId (GEMMA4_E2B, GEMMA3_1B, NONE)`, `DownloadState`

### `core/data`
- Room database (`PalabritaDatabase`)
- Entities: `PuzzleEntity`, `PlayerStatsEntity`, `GameSessionEntity`, `ChatMessageEntity`, `ModelConfigEntity`
- DAOs: `PuzzleDao`, `PlayerStatsDao`, `GameSessionDao`, `ChatMessageDao`, `ModelConfigDao`
- Repository implementations
- `StaticPuzzleProvider`: loads puzzles from the pre-bundled dataset (assets JSON)

### `core/ai`
- `LlmEngineManager`: singleton, Engine lifecycle (init/destroy)
- `PuzzleGenerator`: batch generation with model-specific prompts
- `ChatEngine`: post-guess conversation
- `PromptTemplates`: prompt constants, model variants
- `LlmResponseParser`: JSON parse, regex fallback
- `PuzzleValidator`: deterministic validation
- `StaticPuzzleProvider` (if not coupled in data)

### `core/common`
- `DeviceCapabilities`: RAM detection, tier classification
- `StorageChecker`: available storage
- `StateMachine<S, E>`: generic mini state machine (~30 lines), used in complex flows
- `TextNormalizer`: NFD-based accent normalization (accented chars → ASCII, e.g. "ação" → "acao")
- Shared extension functions

**When to use StateMachine vs sealed class + when:**
- **Formal StateMachine**: Engine lifecycle, model download, onboarding flow (many states, conditional transitions)
- **Sealed class + when**: Game status, chat status (few states, simple transitions)

### `feature/onboarding`
- Welcome screens, language selection, model selection, download, initial generation
- `OnboardingViewModel`

### `feature/game`
- Game screen, letter grid, virtual keyboard, hints system
- `GameViewModel`

### `feature/chat`
- Post-guess chat with LLM, static fallback for Light mode
- `ChatViewModel`

### `feature/settings`
- Settings: language, model, statistics, storage
- `SettingsViewModel` (uses `ResetProgressUseCase` for reset logic)

### `core/testing` (test-only module)
- Shared test fakes: `FakeStatsRepository`, `FakePuzzleRepository`, `FakeGameSessionRepository`, `FakeChatRepository`, `FakeModelRepository`, `FakeLlmEngineManager`, `FakeLlmSession`, `FakeGenerationScheduler`, `FakeAppPreferences`
- Used by all feature module tests to avoid duplicating fakes

## Acceptance Criteria

- [ ] Project compiles with all modules
- [ ] Dependency graph respects the rules (no feature→feature)
- [ ] Hilt injects dependencies correctly across modules
- [ ] Navigation Compose navigates between all routes
- [ ] Version catalog centralizes all dependency versions
- [ ] Incremental build works (changing one module does not recompile everything)
