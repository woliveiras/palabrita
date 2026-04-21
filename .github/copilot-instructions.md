# Project Instructions

## Workflow

This project follows **Spec Driven Development**:

1. **Specs first** — write specs before code
2. **Tests from specs** — each acceptance criterion becomes a test
3. **Code to pass tests** — implement until tests pass, do not modify tests
4. **Docs if needed** — update `docs/` after implementation

Specs and tests are the source of truth. Code adapts to them, never the other way around.

## Project Overview

- **What**: Palabrita — an Android word-guessing game (Wordle-style) powered by on-device LLM (Gemma 4 E2B / Gemma 3 1B via LiteRT-LM)
- **Stack**: Kotlin, Jetpack Compose, Hilt, Room, Navigation Compose, WorkManager, LiteRT-LM SDK, Play Asset Delivery
- **Architecture**: Multi-module MVVM with UDF (Unidirectional Data Flow). StateMachine<S, E> for complex flows, sealed class + when for simple flows.
- **Min SDK**: Android 12 (API 31)
- **3 Operating Modes**: AI Premium (RAM ≥8GB, Gemma 4), AI Compact (4-8GB RAM, Gemma 3), Light (RAM <4GB, static dataset)

## Conventions

- Follow existing patterns in the codebase
- Use Conventional Commits: `feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`
- Keep functions focused — one function, one job
- Prefer explicit over clever
- LLM prompts are written in **English** with `{language}` parameter for output language

## Directory Structure

```
app/                  # Application module (DI, navigation, theme)
core/common/          # StateMachine, DeviceCapabilities, shared utilities
core/model/           # Domain models, repositories interfaces
core/data/            # Room entities, DAOs, repository implementations
core/ai/              # LlmEngineManager, PuzzleGenerator, parsers
feature/onboarding/   # Onboarding flow
feature/game/         # Game screen, difficulty picker, keyboard
feature/chat/         # Post-game chat with LLM
feature/settings/     # Settings, stats, model management
specs/                # Feature specifications (source of truth)
docs/                 # Architecture and integration docs
```

## Build & Test

```bash
./gradlew build                    # Build all modules
./gradlew test                     # Run unit tests
./gradlew connectedAndroidTest     # Run instrumented tests
./gradlew spotlessCheck            # Check formatting
./gradlew spotlessApply            # Fix formatting
```
