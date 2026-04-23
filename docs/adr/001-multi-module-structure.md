# ADR 001 — Multi-Module Gradle Project Structure

## Context

Palabrita is an Android app that will grow to include multiple features (onboarding, game, chat, settings) plus core infrastructure (AI engine, data persistence, device capabilities). We need a project structure that enforces separation of concerns, enables incremental builds, and supports long-term maintainability.

## Decision

Adopt a multi-module Gradle structure with 9 modules:

```
app/                  → Application entry point, navigation, DI root
core/common/          → StateMachine, DeviceCapabilities (no internal deps)
core/model/           → Domain models, repository interfaces (pure Kotlin + serialization)
core/data/            → Room database, entities, DAOs, Hilt DI module
core/ai/              → LLM engine wrapper, puzzle generation (depends on model + common)
feature/onboarding/   → Onboarding flow (Compose + Hilt)
feature/game/         → Game screen (Compose + Hilt)
feature/chat/         → Post-game chat (Compose + Hilt)
feature/settings/     → Settings screen (Compose + Hilt)
```

### Dependency rules

- `app` → all features + all core modules
- `feature/*` → `core/*` only (no feature-to-feature deps)
- `core/data` → `core/model`
- `core/ai` → `core/model` + `core/common`
- `core/common` → nothing (zero internal deps)
- `core/model` → nothing (pure Kotlin)

### Key technology choices

| Concern | Choice | Rationale |
|---|---|---|
| Build system | Gradle 8.13 + Version Catalog | Pinned deps, reproducible builds |
| Kotlin | 2.1.x + KSP | Latest stable, KSP for annotation processing |
| DI | Hilt | Standard for Android, integrates with ViewModel/WorkManager |
| UI | Jetpack Compose + Material 3 | Modern declarative UI |
| Navigation | Navigation Compose (type-safe routes) | Kotlin serialization for route args |
| Persistence | Room | Reactive queries via Flow, compile-time SQL verification |
| State | StateMachine<S,E> (custom, ~50 lines) | No external lib, formal transitions for complex flows |
| Formatting | Spotless + ktfmt (Google style) | Consistent formatting, CI-friendly |

## Consequences

- **Positive**: Enforced module boundaries, faster incremental builds, testable in isolation, scalable for new features
- **Negative**: More boilerplate (build files per module), initial setup cost
- **Trade-off**: Accepted — the structure scales well as the project grows and keeps each module independently maintainable
