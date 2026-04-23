# Contributing to Palabrita

Thank you for your interest in contributing. This document explains how to get started, the development workflow, and the standards we follow.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Standards](#code-standards)
- [Submitting a Pull Request](#submitting-a-pull-request)
- [Reporting Bugs](#reporting-bugs)
- [Requesting Features](#requesting-features)

## Getting Started

1. **Fork** the repository and clone your fork:
   ```bash
   git clone https://github.com/<your-username>/palabrita.git
   cd palabrita
   ```

2. **Set up your environment** — see [README.md](README.md#setup) for requirements (JDK 21, Android SDK 35).

3. **Build and verify** everything works:
   ```bash
   ./gradlew assembleDebug
   ./gradlew test
   ```

## Development Workflow

This project follows **Spec Driven Development**:

1. **Specs first** — feature behaviour is defined in [`specs/`](specs/) before any code is written.
2. **Tests from specs** — each acceptance criterion maps to at least one test.
3. **Code to pass tests** — implement until tests pass; do not modify tests to pass code.
4. **Docs if needed** — update [`docs/`](docs/) after implementation if the architecture changes.

> Specs and tests are the source of truth. Code adapts to them, not the other way around.

### Branch naming

```
feat/<short-description>
fix/<short-description>
chore/<short-description>
docs/<short-description>
```

### Commit messages

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(game): add hint reveal animation
fix(ai): handle null response from LLM parser
chore(deps): bump room to 2.7.1
docs(adr): add ADR 005 for offline-first strategy
```

- Use imperative mood: "add", not "added" or "adds"
- Keep the subject line under 72 characters
- Breaking changes: add `!` — `feat!: remove legacy Light mode fallback`

## Code Standards

### Kotlin

- **Architecture**: MVVM + UDF — `ViewModel` exposes `StateFlow<UiState>`, never `LiveData`
- **DI**: Hilt with constructor injection — never field injection
- **State**: complex flows use `StateMachine<S, E>` from `core/common`; simple flows use `sealed class + when`
- **Formatting**: Spotless + ktfmt (Google style) — run `./gradlew spotlessApply` before committing

### Module boundaries

- `feature/*` modules may only depend on `core/*` — no feature-to-feature dependencies
- `core/model` must remain pure Kotlin with no Android dependencies
- New features belong in a new `feature/<name>` module

### Testing

- Unit tests live in `src/test/`; instrumented tests in `src/androidTest/`
- Test names use backtick notation: `` fun `returns error when input is blank`() ``
- Use `Truth` for assertions, `Turbine` for Flow testing
- New behaviour requires tests before the PR is merged

### Static analysis

Before opening a PR, ensure all checks pass locally:

```bash
./gradlew spotlessCheck   # formatting
./gradlew detekt          # static analysis
./gradlew lint            # Android lint
./gradlew test            # unit tests
```

## Submitting a Pull Request

1. Open a PR against `main`.
2. Fill in the pull request template completely.
3. Ensure CI is green — all jobs in `.github/workflows/ci.yml` must pass.
4. Keep PRs focused — one concern per PR makes review faster.
5. Be responsive to review comments; PRs idle for 14 days may be closed.

## Reporting Bugs

Use the [Bug Report](.github/ISSUE_TEMPLATE/bug_report.yml) issue template. Include:
- Device model and Android version
- Steps to reproduce
- Expected vs. actual behaviour
- Relevant logs (`adb logcat | grep -i palabrita`)

## Requesting Features

Use the [Feature Request](.github/ISSUE_TEMPLATE/feature_request.yml) issue template. Features must align with the project's scope — an on-device AI word game for Android.

## Questions

Open a [Discussion](https://github.com/woliveiras/palabrita/discussions) rather than an issue for general questions.
