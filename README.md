# Palabrita

[![CI](https://github.com/woliveiras/palabrita/actions/workflows/ci.yml/badge.svg)](https://github.com/woliveiras/palabrita/actions/workflows/ci.yml)
[![Min SDK](https://img.shields.io/badge/Android-12%2B-green.svg)](https://developer.android.com/about/versions/12)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF.svg)](https://kotlinlang.org)

A Wordle-style word-guessing game powered by an on-device LLM (Gemma 4 E2B / Qwen / Open Source Models). Generates puzzles and explanations entirely on-device — 100% offline after initial model download.

## Requirements

| Tool | Version | Notes |
|---|---|---|
| JDK | 21+ | Any distribution (OpenJDK, Temurin, etc.) |
| Android SDK | API 31+ (Android 12) | `sdkmanager` or Android Studio |
| Android SDK Build-Tools | Latest | |
| Android SDK Platform | 36 | compileSdk |
| Kotlin | Latest stable | Managed by Gradle wrapper |

> You do **not** need Android Studio. VS Code + terminal works fine.

## Setup

```bash
# 1. Clone
git clone https://github.com/woliveiras/palabrita.git
cd palabrita

# 2. Ensure ANDROID_HOME is set
export ANDROID_HOME="$HOME/Library/Android/sdk"   # macOS
export ANDROID_HOME="$HOME/Android/Sdk"            # Linux

# 3. Install required SDK components (if missing)
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

# 4. Build
./gradlew assembleDebug

# 5. Run on emulator (no Android Studio needed)
emulator @YOUR_AVD_NAME &
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.woliveiras.palabrita/.MainActivity
```

## Build Commands

```bash
./gradlew assembleDebug            # Debug APK
./gradlew assembleRelease          # Release APK
./gradlew test                     # Unit tests
./gradlew connectedAndroidTest     # Instrumented tests (needs emulator/device)
./gradlew spotlessCheck            # Check formatting
./gradlew spotlessApply            # Fix formatting
./gradlew dependencies             # Show dependency tree
```

## Project Structure

```
app/                  → Entry point, navigation, DI root, theme
core/
  common/             → StateMachine, DeviceCapabilities, shared utilities
  model/              → Domain models, repository interfaces (pure Kotlin)
  data/               → Room entities, DAOs, repository implementations
  ai/                 → LiteRT-LM wrapper, PuzzleGenerator, parsers
feature/
  onboarding/         → Onboarding flow, model selection, download
  game/               → Game screen, difficulty picker, keyboard
  chat/               → Post-game chat with LLM
  settings/           → Settings, stats, model management
specs/                → Feature specifications (source of truth)
docs/                 → Architecture docs, ADRs, integration guides
```

## Architecture

- **Multi-module MVVM** with Unidirectional Data Flow (UDF)
- **StateMachine<S, E>** for complex flows (engine lifecycle, download, onboarding)
- **Sealed class + when** for simple state (game status, chat status)
- **Hilt** for DI, **Room** for persistence, **WorkManager** for background puzzle generation
- **2 Operating Modes**: AI Premium (Gemma 4, RAM ≥8GB), AI Compact (Qwen3 0.6B, <8GB)

See [docs/](docs/) for detailed architecture documentation.
See [specs/](specs/) for feature specifications.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the development workflow, code standards, and how to submit a pull request.

## License

See [LICENSE](LICENSE).