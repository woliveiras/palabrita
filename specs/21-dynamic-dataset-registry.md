# Spec: Dynamic Dataset Registry

## Context & Motivation

Today, the list of available puzzle languages is **hardcoded** in three separate places:

1. `LanguageSelectionScreen.kt` — `LANGUAGES` list for Settings UI
2. `OnboardingScreen.kt` — three `LanguageCard(...)` calls
3. `PromptTemplates.kt` — `LANGUAGE_NAMES` map for LLM prompt display names

Adding a new language (e.g., Italian) requires modifying Kotlin code in at least three files,
adding string resources, and updating the fallback hint provider — a high barrier for
open-source contributors who may not know Kotlin or the project structure.

**Goal:** Make the list of puzzle languages **dynamic**, driven entirely by data files. An
open-source contributor should be able to add a new language by creating **two files** (a
word list JSON + a manifest entry) and opening a PR — zero Kotlin changes required.

This spec only affects the **game word language** (which dataset to play). The **UI language**
(Android locale for translations) remains fixed to the locales the app ships string resources
for (pt, en, es).

## How It Works

### Dataset Manifest

A new file `core/ai/src/main/resources/wordlists/manifest.json` becomes the **single source
of truth** for available puzzle languages:

```json
[
  {
    "code": "pt",
    "displayName": "Português",
    "flag": "🇧🇷",
    "promptName": "Brazilian Portuguese"
  },
  {
    "code": "en",
    "displayName": "English",
    "flag": "🇺🇸",
    "promptName": "English"
  },
  {
    "code": "es",
    "displayName": "Español",
    "flag": "🇪🇸",
    "promptName": "Spanish"
  }
]
```

| Field | Purpose | Example |
|-------|---------|---------|
| `code` | ISO 639-1 code, matches the `{code}.json` wordlist filename | `"it"` |
| `displayName` | Human-readable name shown in language selection UI | `"Italiano"` |
| `flag` | Emoji flag for visual identification in the UI | `"🇮🇹"` |
| `promptName` | Full language name sent to LLM in prompts | `"Italian"` |

### DatasetRegistry

A new class `DatasetRegistry` in `core/ai` reads and caches the manifest:

```kotlin
class DatasetRegistry @Inject constructor() {

  data class DatasetInfo(
    val code: String,
    val displayName: String,
    val flag: String,
    val promptName: String,
  )

  fun availableLanguages(): List<DatasetInfo>
  fun findByCode(code: String): DatasetInfo?
  fun promptName(code: String): String  // Falls back to code if not found
}
```

### Integration Points

| Consumer | Before | After |
|----------|--------|-------|
| `PromptTemplates.languageDisplayName()` | Reads from hardcoded `LANGUAGE_NAMES` map | Delegates to `DatasetRegistry.promptName()` |
| `OnboardingScreen` (language step) | 3× hardcoded `LanguageCard(...)` | Iterates `registry.availableLanguages()` |
| `LanguageSelectionScreen` (settings) | Hardcoded `LANGUAGES` list | Iterates `registry.availableLanguages()` |
| `HintFallbackProvider` | `when(language)` with hardcoded pt/en/es | Generic English fallback for unknown languages; pt/en/es keep their specific fallbacks |
| `WordList` | Already dynamic — loads `/wordlists/{code}.json` | **No change** |

### Contributor Workflow

To add a new puzzle language (e.g., Italian):

1. Create `core/ai/src/main/resources/wordlists/it.json` following the existing format
2. Add an entry to `manifest.json`
3. Open a PR — CI validates the dataset automatically

**Zero Kotlin code. Zero Room migration. Zero string resources.**

### What Does NOT Change

- Word list file format (`{length → words[]}` JSON)
- Room schema — `Puzzle.language` is already a free `String`
- Room queries — already filter by any `language` value
- UI language selection (Android locale) — stays fixed to pt/en/es
- Puzzle generation pipeline — `GeneratePuzzlesUseCase` and `PuzzleGenerator` are unchanged
- `WordList` object — already loads any `{code}.json` dynamically

## Requirements

### Functional

#### Manifest & Registry

- [ ] Create `manifest.json` in `core/ai/src/main/resources/wordlists/` with entries for pt, en, es
- [ ] `DatasetRegistry` parses `manifest.json` from classpath resources on first access
- [ ] `DatasetRegistry` caches parsed result in memory (read once)
- [ ] `DatasetRegistry.availableLanguages()` returns `List<DatasetInfo>` in manifest order
- [ ] `DatasetRegistry.findByCode(code)` returns `DatasetInfo?` for a given language code
- [ ] `DatasetRegistry.promptName(code)` returns the `promptName` or falls back to the raw code
- [ ] If `manifest.json` is missing or malformed, `availableLanguages()` returns an empty list (fail safe)
- [ ] Each manifest entry must have a corresponding `{code}.json` wordlist; `DatasetRegistry` only returns entries where the wordlist file exists

#### Prompt Integration

- [ ] `PromptTemplates.languageDisplayName()` delegates to `DatasetRegistry.promptName()` instead of reading from hardcoded `LANGUAGE_NAMES` map
- [ ] Remove the hardcoded `LANGUAGE_NAMES` map from `PromptTemplates`

#### Fallback Hints

- [ ] `HintFallbackProvider` keeps specific fallbacks for pt, en, es
- [ ] For any language not in the `when` block, `HintFallbackProvider` uses the English fallback (existing `else` branch) — no change needed
- [ ] No new fallback entries are required when adding a language via manifest

#### Onboarding UI

- [ ] `OnboardingScreen` language step reads available languages from `DatasetRegistry`
- [ ] Language cards are rendered dynamically from the registry list
- [ ] Each card shows the `flag` and `displayName` from the manifest
- [ ] The language code (`DatasetInfo.code`) is passed to `OnboardingAction.SelectLanguage`
- [ ] If only one language is available, it is auto-selected and the step is skipped

#### Settings UI

- [ ] `LanguageSelectionScreen` "Game Words Language" section reads from `DatasetRegistry`
- [ ] Remove the hardcoded `LANGUAGES` list from `LanguageSelectionScreen`
- [ ] The "App Language" section remains hardcoded (tied to Android string resources, not datasets)
- [ ] Each game language option shows `flag` + `displayName`

#### CI Validation

- [ ] A CI script validates all dataset files on every PR
- [ ] Validation checks: manifest is valid JSON array, each entry has all required fields (`code`, `displayName`, `flag`, `promptName`)
- [ ] Validation checks: each manifest entry has a matching `{code}.json` wordlist file
- [ ] Validation checks: each wordlist file is valid JSON with string keys (word lengths) mapping to string arrays
- [ ] Validation checks: each wordlist has at least 10 words per word length for lengths 4, 5, and 6 (minimum viable dataset)
- [ ] Validation checks: no orphan wordlist files without a manifest entry (warning, not error)
- [ ] Validation checks: no duplicate `code` values in the manifest
- [ ] CI job fails the PR if any validation error is found

#### Documentation

- [ ] `CONTRIBUTING.md` has a new section explaining how to add a puzzle language
- [ ] The guide includes the manifest format, wordlist format, minimum word counts, and a step-by-step example

### Non-Functional

- [ ] Manifest parsing adds negligible startup time (< 5ms for a small JSON file)
- [ ] `DatasetRegistry` is thread-safe (immutable cached result)
- [ ] Word list file format remains backward-compatible (no changes to existing pt/en/es files)

## Acceptance Criteria

### Manifest Parsing

- [ ] Given a valid `manifest.json` with 3 entries, when `availableLanguages()` is called, then it returns 3 `DatasetInfo` objects in manifest order
- [ ] Given a manifest entry with code "it" but no `it.json` wordlist file, when `availableLanguages()` is called, then the "it" entry is excluded
- [ ] Given a missing `manifest.json`, when `availableLanguages()` is called, then it returns an empty list
- [ ] Given a malformed `manifest.json` (invalid JSON), when `availableLanguages()` is called, then it returns an empty list

### Registry Lookup

- [ ] Given a registry with entries for pt, en, es, when `findByCode("pt")` is called, then it returns the pt `DatasetInfo`
- [ ] Given a registry with entries for pt, en, es, when `findByCode("fr")` is called, then it returns null
- [ ] Given a registry with a pt entry where `promptName = "Brazilian Portuguese"`, when `promptName("pt")` is called, then it returns "Brazilian Portuguese"
- [ ] Given a code not in the registry, when `promptName("xx")` is called, then it returns "xx" (raw code fallback)

### Prompt Integration

- [ ] Given a manifest with `promptName = "Italian"` for code "it", when `PromptTemplates.languageDisplayName("it")` is called, then it returns "Italian"
- [ ] Given a manifest with pt/en/es entries, when `PromptTemplates.languageDisplayName("pt")` is called, then it returns "Brazilian Portuguese" (same as before, but now from manifest)

### Onboarding UI

- [ ] Given a registry with 3 languages, when the language step renders, then 3 language cards are displayed
- [ ] Given a registry with 4 languages (including "it"), when the language step renders, then 4 language cards are displayed including Italian
- [ ] Given a registry with 1 language, when the language step renders, then the language is auto-selected and the step is skipped
- [ ] Given a language card, when displayed, then it shows the flag emoji and display name from the manifest

### Settings UI

- [ ] Given a registry with 3 languages, when the game language section renders, then 3 options are displayed
- [ ] Given a registry with 4 languages, when the game language section renders, then 4 options are displayed
- [ ] Given the app language section, when it renders, then it still shows only pt/en/es (unchanged, not from registry)

### HintFallbackProvider

- [ ] Given language "pt", when fallback hints are generated, then they are in Portuguese (existing behavior)
- [ ] Given language "it" (not in the when block), when fallback hints are generated, then they use the English fallback

### CI Validation

- [ ] Given a valid manifest and valid wordlists, when CI runs, then validation passes
- [ ] Given a manifest entry with missing `promptName` field, when CI runs, then validation fails with a descriptive error
- [ ] Given a manifest entry for "fr" but no `fr.json` file, when CI runs, then validation fails
- [ ] Given a wordlist with only 3 words for length 4, when CI runs, then validation fails (minimum 10)
- [ ] Given a wordlist that is not valid JSON, when CI runs, then validation fails
- [ ] Given duplicate codes in manifest, when CI runs, then validation fails

### End-to-End (New Language)

- [ ] Given a contributor adds `it.json` and a manifest entry for Italian, when the app runs, then Italian appears in both onboarding and settings language selection
- [ ] Given Italian is selected, when puzzles are generated, then `WordList` loads words from `it.json` and prompts use "Italian" as the language name
- [ ] Given Italian is selected and LLM fails to generate hints, then fallback hints are in English (generic fallback)

## Edge Cases

- Manifest exists but is an empty array → `availableLanguages()` returns empty list; onboarding language step has no options (blocked state — should not happen with CI validation)
- Wordlist JSON has unexpected keys (e.g., `"3"` for 3-letter words) → ignored; `WordList` only queries lengths 4–8
- Wordlist has a length key with an empty array → `WordList.getWords()` returns empty list for that length; generation skips it
- Multiple entries with the same code in manifest → CI rejects; at runtime, first entry wins
- Contributor adds a wordlist file but forgets the manifest entry → CI warns about orphan file
- Language code contains uppercase or special characters → CI validates ISO 639-1 format (lowercase, 2–3 chars)
- User had "it" selected, then a new app version removes Italian from manifest → `findByCode("it")` returns null; app should fall back to first available language

## Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Manifest location | Classpath resource alongside wordlists | Co-located with datasets; no Android-specific paths needed |
| Manifest format | JSON array | Consistent with wordlist format; easy to validate in CI |
| CI validation tool | Shell script (bash) | Zero dependencies; runs on any CI runner without extra setup |
| `DatasetRegistry` scope | Singleton via `@Singleton @Inject` | Parsed once, shared across all consumers |
| Fallback for unknown languages | English hints | Safe default; LLM prompts already work with any `promptName` |
| Wordlist existence check | Runtime in `DatasetRegistry` | Defense-in-depth; CI catches mismatches first, runtime filters as safety net |
| App Language section | Stays hardcoded | Tied to Android string resource locales, not datasets |
| `PromptProvider` interface | Keep `languageDisplayName` method | Interface unchanged; only implementation changes to use registry |

## Out of Scope

- UI language (Android locale) expansion — adding new app translations requires string resources, not just datasets
- Remote/downloadable datasets — all datasets ship in the APK via classpath resources
- Dataset versioning or update mechanism
- Word quality scoring or automated word validation
- Accent/diacritic data in the manifest (handled by existing `wordDisplay` logic in puzzle generation)
- Internationalized `displayName` (e.g., showing "Portugués" when UI is Spanish) — display names are always in the language's own name
