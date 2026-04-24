# Spec 04 — Onboarding

## Summary

The onboarding is the user's first contact with the app. It guides the player through the game explanation, language choice, AI model selection (or auto-detection), and download. The flow adapts to the device hardware.

## General Flow

```
App opens (first time)
    │
    ▼
Screen 1: Welcome
    │
    ▼
Screen 2: Language
    │
    ▼
Screen 3: AI Selection ──── RAM < 4GB ────→ Light mode (auto) ──→ Screen 5
    │                                                                    
    ├── "Yes, I want to choose" ──→ Model Picker ──→ Screen 4
    │
    └── "No, choose for me" ──→ Auto-select ──→ Screen 4
                                                        │
                                                        ▼
                                                  Screen 4: Download
                                                        │
                                                        ▼
                                                  Screen 5: Initial Generation
                                                        │
                                                        ▼
                                                  Game screen
```

## Screens

### Screen 1 — Welcome

**Content:**
- Palabrita logo
- Title: "Discover the word of the day"
- Subtitle: "A word game with artificial intelligence, directly on your phone"
- Illustration or simple animation (placeholder in V1)
- Button: "Start"

**Rules:**
- If the user has already completed onboarding (flag in DataStore), skip directly to the game
- Smooth entry animation (fade in)

### Screen 2 — Language Selection

**Content:**
- Title: "Which language do you want to play in?"
- Selectable cards:
  - 🇧🇷 Português
  - 🇺🇸 English
  - 🇪🇸 Español
- Auxiliary text: "You can change this later in settings"
- Button: "Continue"

**Rules:**
- Default language pre-selected based on device locale
- Save to `PlayerStatsEntity.preferredLanguage`
- This is the language of the **GAME WORDS**, not the UI
- The UI follows the operating system language (device locale) via `res/values-pt`, `res/values-en`, `res/values-es` or use en fallback
- The player can change the UI language in settings (independently of the game language)
- Example: phone in English → UI in English, but player chooses Portuguese words

### Screen 3 — AI Selection

**Case A — Device with RAM < 4GB (tier LOW):**

- Title: "Your device"
- Message: "Your device does not support local AI. But don't worry! You'll play with our word bank, which is just as fun!"
- Informational icon (not error)
- Button: "Got it, let's play!" → navigates to Screen 5 (generation skips directly, loads static dataset)
- Save `ModelConfig(modelId = "none")`

**Case B — Device with RAM ≥ 4GB:**

- Title: "Do you want to choose your AI?"
- Two buttons:
  - **"Yes, I want to choose"** → expands Model Picker (inline or new screen)
  - **"No, choose for me"** → auto-select and navigate to Screen 4

**Model Picker (expanded from "Yes"):**

- Title: "Choose how your AI should behave!"
- Card 1: **"I don't need to save space"**
  - Subtitle: "Gemma 4 E2B · ~2.6 GB download"
  - Info: "Requires 8 GB of RAM"
  - Badge: "Recommended" (if tier HIGH)
  - Icon: star or rocket
- Card 2: **"I need to save space"**
  - Subtitle: "Gemma 3 1B · ~529 MB download"
  - Info: "Requires 4 GB of RAM"
  - Badge: "Recommended" (if tier MEDIUM)
  - Icon: leaf or lightweight
- Button: "Continue"

**Warning (if user chooses model above tier):**
- Dialog: "Your device has {X} GB of RAM. The selected model requires {Y} GB. Performance may be poor or the app may crash. Do you want to continue?"
- Buttons: "Continue anyway" / "Choose another"

**Auto-select logic:**
- RAM ≥ 8GB → Gemma 4 E2B
- 4GB ≤ RAM < 8GB → Gemma 3 1B

### Screen 4 — Model Download

**Content:**
- Title: "Preparing your AI"
- Info box: "The AI model will be downloaded to your phone. After that, everything will work offline!"
- Details:
  - Model: {name}
  - Size: {size}
  - Available space: {available}
- Progress bar (determinate, with %)
- **Curiosity Slider**: while downloading, a slider with facts about local/offline/open-source AI auto-advances every 4 seconds with fade transitions and dot indicators (replaced the old spinner)
- "Continue" button appears when download completes (no auto-navigation)
- Button "Cancel"

**Rules:**
- Check connection before starting
  - If Wi-Fi: start automatically
  - If mobile data + model > 500MB: dialog "The download is {size}. Do you want to download using mobile data?"
  - If no connection: "Connect to the internet to download the model"
- If insufficient space: "Insufficient space. You need at least {size + margin} available."
- Download via Play Asset Delivery (Play Store) or direct download (dev)
- Save progress to `ModelConfigEntity.downloadState`
- If app is closed during download: resume on reopen (PAD supports resume)
- If download fails: button "Try again" + option "Choose smaller model"

**State Machine — Model Download:**

```
┌─────────┐
│  IDLE   │
└────┬────┘
     │ StartDownload
     ▼
┌──────────────┐    WifiRequired    ┌─────────────────┐
│  CHECKING    │──────────────────→│ WAITING_FOR_WIFI │
└──────┬───────┘                   └────────┬─────────┘
       │ SpaceOk                            │ WifiConnected
       ▼                                    ▼
┌──────────────┐                   ┌──────────────┐
│ DOWNLOADING  │←──────────────────│ DOWNLOADING  │
└──┬────────┬──┘                   └──────────────┘
   │        │
 Done    Failed
   │        │
   ▼        ▼
┌────────┐ ┌────────┐
│COMPLETE│ │ FAILED │
└────────┘ └───┬────┘
               │ Retry ──→ CHECKING
               │ Cancel ──→ IDLE
```

```kotlin
// Download state machine
sealed class DownloadState {
    data object Idle : DownloadState()
    data object Checking : DownloadState()
    data object WaitingForWifi : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Completed(val modelPath: String) : DownloadState()
    data class Failed(val errorCode: Int) : DownloadState()
}

sealed class DownloadEvent {
    data object StartDownload : DownloadEvent()
    data object SpaceOk : DownloadEvent()
    data object WifiRequired : DownloadEvent()
    data object WifiConnected : DownloadEvent()
    data class Progress(val percent: Float) : DownloadEvent()
    data object Done : DownloadEvent()
    data class Fail(val errorCode: Int) : DownloadEvent()
    data object Retry : DownloadEvent()
    data object Cancel : DownloadEvent()
}
```

### Screen 5 — Initial Puzzle Generation

**Content (AI mode):**
- Title: "Generating your first challenges..."
- Subtitle: "This only happens the first time"
- Live activity feed showing generation states: CREATING → VALIDATING → ACCEPTED / FAILED_RETRYING (see Spec 13)
- Progress: "Puzzle 3 of 5..."
- Subtle animation (loading with personality)

**Content (Light mode):**
- Title: "Preparing the game..."
- Subtitle: "Loading words from the curated database"
- Quick progress (< 1s, asset loading)

**Rules (AI mode):**
- Generate 5 puzzles of 4 letters in the selected language (Level 1 — see Spec 15)
- Use `PuzzleGenerator.generateBatch(count=5, wordLength=4, ...)`
- Show progress per puzzle via live activity feed
- If a puzzle fails validation after retries: skip and continue
- Minimum acceptable: 3 valid puzzles (if < 3, show error and offer retry or Light mode)
- On completion: save puzzles to Room, navigate to Home

> **Note:** Language is saved to `PlayerStatsEntity.preferredLanguage` BEFORE generation starts (race condition fix — Spec 16). Prompts use language display names ("Brazilian Portuguese", not "pt").

**Rules (Light mode):**
- Load static dataset from assets
- Filter by selected language
- Save to Room with `source = STATIC`
- Immediate navigation to Game

## OnboardingViewModel

**State Machine — Onboarding Flow:**

```
WELCOME ──→ LANGUAGE ──→ MODEL_SELECTION ──→ DOWNLOAD ──→ GENERATION ──→ COMPLETE
                              │                                ▲
                              │ (tier LOW)                     │
                              └────────────────────────────────┘
                                (skips download, goes directly to generation/static load)
```

```kotlin
// Onboarding state machine
val onboardingStateMachine = StateMachine<OnboardingStep, OnboardingEvent>(
    initialState = OnboardingStep.WELCOME,
    transitions = mapOf(
        (OnboardingStep.WELCOME to OnboardingEvent.Next) to OnboardingStep.LANGUAGE,
        (OnboardingStep.LANGUAGE to OnboardingEvent.Next) to OnboardingStep.MODEL_SELECTION,
        (OnboardingStep.MODEL_SELECTION to OnboardingEvent.Next) to OnboardingStep.DOWNLOAD,
        (OnboardingStep.MODEL_SELECTION to OnboardingEvent.SkipToLight) to OnboardingStep.GENERATION,
        (OnboardingStep.DOWNLOAD to OnboardingEvent.DownloadComplete) to OnboardingStep.GENERATION,
        (OnboardingStep.GENERATION to OnboardingEvent.GenerationComplete) to OnboardingStep.COMPLETE,
        // Back navigation
        (OnboardingStep.LANGUAGE to OnboardingEvent.Back) to OnboardingStep.WELCOME,
        (OnboardingStep.MODEL_SELECTION to OnboardingEvent.Back) to OnboardingStep.LANGUAGE,
    )
)
```

**State:**

```kotlin
data class OnboardingState(
    val currentStep: OnboardingStep,
    val selectedLanguage: String = Locale.getDefault().language,
    val deviceTier: DeviceTier,
    val selectedModel: ModelId? = null,
    val downloadState: DownloadState = DownloadState.Idle,
    val generationProgress: GenerationProgress? = null,
    val error: OnboardingError? = null
)

enum class OnboardingStep {
    WELCOME, LANGUAGE, MODEL_SELECTION, DOWNLOAD, GENERATION, COMPLETE
}

data class GenerationProgress(
    val current: Int,
    val total: Int,
    val lastGeneratedWord: String? = null
)
```

**Actions:**

```kotlin
sealed class OnboardingAction {
    data object StartOnboarding : OnboardingAction()
    data class SelectLanguage(val language: String) : OnboardingAction()
    data class SelectModel(val modelId: ModelId) : OnboardingAction()
    data object AutoSelectModel : OnboardingAction()
    data object StartDownload : OnboardingAction()
    data object CancelDownload : OnboardingAction()
    data object RetryDownload : OnboardingAction()
    data object StartGeneration : OnboardingAction()
    data object SkipToLightMode : OnboardingAction()
}
```

## Edge Cases

| Scenario | Behavior |
|---|---|
| App closed during download | PAD resumes; direct download restarts (show saved progress) |
| App closed during generation | Partial puzzles remain in Room; on reopen, check if ≥3 exist and complete if necessary |
| No internet | Block Screen 4; offer Light mode as an alternative |
| Download complete but corrupted model | Detected on Engine initialization; offer re-download |
| User goes back to previous screen | Cancel in-progress download/generation |
| Space runs out during download | PAD handles it; direct download: detect IOException, alert user |

## Acceptance Criteria

- [ ] Full onboarding works on tier HIGH device (Gemma 4 download + generation)
- [ ] Full onboarding works on tier MEDIUM device (Gemma 3 download + generation)
- [ ] Full onboarding works on tier LOW device (Light mode, no download)
- [ ] Auto-select chooses the correct model based on RAM
- [ ] Warning appears when user chooses model above tier
- [ ] Download shows real progress (not fake)
- [ ] Download can be cancelled
- [ ] Download resumes after app is closed (PAD)
- [ ] Generation produces ≥3 valid puzzles
- [ ] If generation fails completely, offers Light mode as fallback
- [ ] Onboarding does not reappear after being completed
- [ ] Selected language persists in PlayerStats
- [ ] Wi-Fi warning appears for downloads >500MB on mobile data
