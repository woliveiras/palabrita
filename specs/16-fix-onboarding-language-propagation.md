# Spec: Fix Onboarding Language Propagation

## Context & Motivation

During onboarding, the user selects their preferred language (PT/EN/ES) for puzzle words. However, a race condition causes the `PuzzleGenerationWorker` to start **before** the chosen language is persisted to the database. The Worker reads `statsRepository.getStats().preferredLanguage`, which may still hold the default value (`"pt"`).

Additionally, the LLM prompts pass raw ISO codes (`"pt"`, `"en"`, `"es"`) as the language parameter. Small on-device models (Qwen3 0.6B, Gemma 4 E2B) can confuse Portuguese and Spanish when given only a two-letter code, since both are Romance languages with overlapping vocabulary.

**Observed bug**: user selected Portuguese (pt-BR) but received words in Spanish.

**Root causes**:
1. `enqueueBackgroundGeneration()` is called **before** `statsRepository.updateLanguage()` (line 200 vs 201 in `OnboardingViewModel.kt`)
2. LLM prompts use ambiguous short codes (`"pt"`) instead of explicit language names

## Requirements

### Functional

- [ ] Language preference MUST be persisted to the database before puzzle generation is scheduled
- [ ] LLM prompts MUST use unambiguous full language names instead of ISO codes
- [ ] The language mapping must cover all supported languages: pt → "Brazilian Portuguese", en → "English", es → "Spanish"
- [ ] `PuzzleGenerationScheduler.scheduleGeneration()` must accept the language as a parameter to guarantee the correct value is used

### Non-Functional

- [ ] No additional latency: `updateLanguage()` is a single Room upsert (~1ms), negligible
- [ ] Backward-compatible: existing saved languages ("pt", "en", "es") must map correctly to display names in prompts

## Acceptance Criteria

- [ ] Given the user selects "pt" in onboarding, when the engine becomes ready, then `statsRepository.updateLanguage("pt")` completes before `enqueueBackgroundGeneration()` is called
- [ ] Given language "pt" is passed to `puzzleUserPromptLarge()`, then the prompt text contains "Brazilian Portuguese", not "pt"
- [ ] Given language "en" is passed to `puzzleUserPromptLarge()`, then the prompt text contains "English", not "en"
- [ ] Given language "es" is passed to `puzzleUserPromptLarge()`, then the prompt text contains "Spanish", not "es"
- [ ] Given language "pt" is passed to `puzzlePromptCompact()`, then the prompt text contains "Brazilian Portuguese"
- [ ] Given language "en" is passed to `puzzlePromptCompact()`, then the prompt text contains "English"
- [ ] Given language "es" is passed to `puzzlePromptCompact()`, then the prompt text contains "Spanish"
- [ ] Given an unknown language code "fr", then the prompt falls back to the raw code "fr"
- [ ] Given language "pt" is passed to `chatSystemPrompt()`, then the response instruction contains "Brazilian Portuguese"

## Edge Cases

- What if the system locale returns an unsupported language (e.g., "fr", "de")? → Fall back to the raw code; the LLM will attempt best-effort generation
- What if `updateLanguage()` fails? → Worker should still run with whatever is in the DB (safe default "pt")
- What if the Worker starts on a different process before the DB write flushes? → Room upsert is synchronous within its transaction; once `updateLanguage()` suspends back, the value is durable

## Decisions

| Decision | Choice | Reasoning |
|----------|--------|-----------|
| Language mapping location | `PromptTemplates.languageDisplayName()` | Keep prompt logic centralized; single source of truth for code→name mapping |
| Mapping strategy | Map with fallback to raw code | Supports future languages without breaking; forward-compatible |
| Fix ordering in ViewModel | Save language, then schedule generation | Simple, no architectural change needed — just swap 2 lines + add `suspend` sequencing |
| Pass language to scheduler | Yes, as explicit parameter | Eliminates any reliance on DB state at schedule time; Worker still reads from DB but the value is guaranteed to be there |

## Out of Scope

- Adding new languages beyond PT/EN/ES
- Validating that generated words actually belong to the requested language (post-generation validation)
- Changing the language selection UI
