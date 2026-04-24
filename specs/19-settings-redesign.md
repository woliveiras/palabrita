# Spec 19 — Settings Screen Redesign

## Context & Motivation

The Settings screen needs a visual and functional redesign to match the new UI design (three sections: AI CONFIGURATION, LANGUAGE & REGION, ABOUT). Stats, storage info, delete model, and reset progress are removed from this release. Two new dedicated screens are introduced: Language Selection and Model Download.

Reference design screenshots show:
- AI CONFIGURATION: "AI Model" row (current model subtitle) + "Regenerate Puzzles" row
- LANGUAGE & REGION: "Language" row (current language subtitle)
- ABOUT: "About AI Model" row + App Version static row

## Requirements

### Functional

#### Settings Screen
- [ ] Shows three sections: AI CONFIGURATION, LANGUAGE & REGION, ABOUT
- [ ] "AI Model" row shows current model display name as subtitle; tapping opens model picker bottom sheet
- [ ] "Regenerate Puzzles" row navigates to GenerationScreen (isRegeneration = true)
- [ ] "Language" row shows current game language as subtitle; tapping navigates to LanguageSelectionScreen
- [ ] "About AI Model" row navigates to AiInfoScreen
- [ ] App Version row shows current app version (static, non-clickable)
- [ ] Stats, Storage Info, Delete Model and Reset Progress are removed

#### Model Picker Bottom Sheet
- [ ] Lists all 6 models (GEMMA4_E4B, GEMMA4_E2B, PHI4_MINI, DEEPSEEK_R1_1_5B, QWEN2_5_1_5B, QWEN3_0_6B) plus NONE (Light Mode)
- [ ] Each option shows: display name, human-readable size (MB or GB), RAM requirement
- [ ] Currently selected model is visually highlighted with a star icon
- [ ] Models already downloaded can be selected directly (no download needed)
- [ ] Selecting a downloaded model updates the model config immediately and closes the picker
- [ ] Selecting a non-downloaded model navigates to ModelDownloadScreen

#### Model Download Screen (feature/settings)
- [ ] Shows model name, file size, and a progress bar
- [ ] "Start Download" button initiates download via ModelDownloadManager
- [ ] Download progress (0–100%) is shown in real time
- [ ] "Cancel" button stops the download and navigates back
- [ ] On download completion:
  - If unplayed puzzle count == 0 for current language → shows "Generate Puzzles" button that navigates to GenerationScreen
  - If unplayed puzzle count > 0 → shows success state and navigates back automatically after 1.5s
- [ ] On failure: shows error message with "Retry" and "Cancel" buttons
- [ ] Updates ModelConfig in repository on success (sets modelId, DOWNLOADED state, path)

#### Language Selection Screen
- [ ] Shows two independent sections: "App Language" and "Game Words Language"
- [ ] App Language: radio group PT/EN/ES — selecting a different app language calls AppCompatDelegate.setApplicationLocales() which recreates the Activity
- [ ] App language selection is persisted via AppPreferences.appLanguage
- [ ] Game Words Language: radio group PT/EN/ES — selecting a different language:
  - Shows a confirmation dialog: "Generate Now" or "Continue Playing, Generate Later"
  - "Generate Now": deletes unplayed puzzles in old language, navigates to GenerationScreen
  - "Generate Later": deletes unplayed puzzles in old language, saves new language preference, navigates back
  - Selecting the same language as current → no-op, no dialog shown
- [ ] Played (isPlayed = true) puzzles are never deleted — only unplayed puzzles for the old language

### Non-Functional

- [ ] All new screens respect Android safe area insets (WindowInsets.systemBars)
- [ ] All new screens support dark and light mode via MaterialTheme.colorScheme tokens only
- [ ] Settings screen scrollable on small screens

## Acceptance Criteria

### Settings ViewModel

- [ ] Given settings screen loads, when ViewModel initializes, then state contains current model config and current game language
- [ ] Given model picker is closed, when ShowModelPicker action is dispatched, then isModelPickerVisible = true
- [ ] Given model picker is open, when DismissModelPicker action is dispatched, then isModelPickerVisible = false
- [ ] Given a model with DOWNLOADED state is selected, when SelectModel action is dispatched, then model config is updated and isModelPickerVisible = false
- [ ] Given a model with NOT_DOWNLOADED state is selected, when SelectModel action is dispatched, then NavigateToModelDownload event is emitted and picker closes
- [ ] Given any state, when RegenPuzzles action is dispatched, then NavigateToGeneration event is emitted
- [ ] Given any state, when NavigateToLanguageSelection action is dispatched, then NavigateToLanguageSelection event is emitted
- [ ] Given any state, when NavigateToAiInfo action is dispatched, then NavigateToAiInfo event is emitted
- [ ] Given availableModels list is loaded, then it contains all ModelId entries from AiModelRegistry (excluding NONE or including it as "Light Mode")

### Model Download ViewModel

- [ ] Given ViewModel initializes with a modelId, then state contains the model info (name, size)
- [ ] Given download has not started, when StartDownload action is dispatched, then download begins and state reflects Downloading progress
- [ ] Given download is in progress, when progress is 50%, then state.downloadProgress == 0.5f
- [ ] Given download completes and unplayed count > 0, then NavigateBack event is emitted
- [ ] Given download completes and unplayed count == 0, then NavigateToGeneration event is emitted with the new modelId
- [ ] Given download fails, when error occurs, then state.errorMessage is non-null
- [ ] Given download is in progress, when CancelDownload is dispatched, then download is cancelled and NavigateBack event is emitted
- [ ] Given download completes, then ModelConfig is updated in ModelRepository with DOWNLOADED state and model path

### Language Selection ViewModel

- [ ] Given ViewModel initializes, then state contains current app language from AppPreferences and current game language from StatsRepository
- [ ] Given user selects a different app language, when ChangeAppLanguage action is dispatched, then appLanguage is saved to AppPreferences and RestartForLocale event is emitted
- [ ] Given user selects the same app language, when ChangeAppLanguage action is dispatched, then no event is emitted
- [ ] Given user selects a different game language, when ChangeGameLanguage action is dispatched, then pendingGameLanguage is set and showConfirmDialog = true
- [ ] Given user selects the same game language, when ChangeGameLanguage action is dispatched, then showConfirmDialog = false and no pending change
- [ ] Given confirm dialog is shown and user picks "Generate Now", then unplayed puzzles in old language are deleted, game language is updated, and NavigateToGeneration event is emitted
- [ ] Given confirm dialog is shown and user picks "Generate Later", then unplayed puzzles in old language are deleted, game language is updated, and NavigateBack event is emitted
- [ ] Given confirm dialog is shown and user dismisses it, then pendingGameLanguage is cleared and showConfirmDialog = false

## Edge Cases

- What if the model file already exists on disk (re-download scenario)? → ModelDownloadManager checks file existence and size; if matching, completes immediately
- What if device runs out of storage during download? → ModelDownloadManager emits Failed with insufficient space message
- What if locale returned by device is not PT/EN/ES? → AppPreferences defaults to "en"; LanguageSelectionScreen shows "English" pre-selected
- What if the user taps Regenerate Puzzles while generation is already running? → GenerationScreen handles deduplication (existing behavior)
- What if game language selected equals current game language? → LanguageSelectionViewModel no-ops, no dialog

## Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Removed sections | Completely removed (Stats, Storage, Delete Model, Reset Progress) | Per product decision; reduces cognitive load on this screen |
| App language storage | DataStore via AppPreferences.appLanguage | Consistent with existing preference storage |
| App language change mechanism | AppCompatDelegate.setApplicationLocales() | Official Android per-app language API (API 33+), with fallback for API 31-32 |
| Model download location | New standalone screen inside feature/settings | Reuses ModelDownloadManager without coupling to Onboarding flow |
| Game language change | Deletes unplayed old-language puzzles in both paths (Generate Now + Generate Later) | Prevents orphaned puzzles that can never be reached; played history preserved |
| AiModelRegistry access | Direct object call in ViewModel | Registry is a pure object with no I/O; no need for DI |

## Out of Scope

- Adding new languages beyond PT/EN/ES
- Stats screen (removed, not relocated)
- Reset progress (removed, not relocated)
- Delete model (removed, not relocated)
- Model download from Onboarding (no changes to OnboardingViewModel)
