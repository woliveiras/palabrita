# ADR 004 — Play Asset Delivery for LLM Model Distribution

## Context

Palabrita's LLM models (Gemma 4 E2B ~2 GB, Gemma 3 1B ~1 GB) cannot be bundled in the APK — the 150 MB install-time limit makes that impossible. We need a strategy to distribute large binary assets to users.

### Constraints

- **Size**: models are 1–2 GB compressed.
- **Offline use**: after initial download, the app must work entirely offline.
- **User experience**: download should be transparent, resumable, and respectful of metered connections.
- **Platform**: Android-only distribution via Google Play.

### Options Evaluated

| Option | Size Limit | Resumable | Conditional | Hosted By |
|---|---|---|---|---|
| **Play Asset Delivery (PAD)** | 2 GB/pack | ✅ | ✅ | Google Play |
| Bundled in APK/AAB | 150 MB | N/A | N/A | Google Play |
| Self-hosted CDN (OkHttp) | Unlimited | Manual | Manual | Us |
| Firebase Storage | Unlimited | Partial | Manual | Google |
| GitHub Releases | 2 GB/file | ❌ | Manual | GitHub |

## Decision

Use **Play Asset Delivery** with `on-demand` delivery type for model asset packs.

## Rationale

1. **Size and hosting**: PAD supports asset packs up to 2 GB, hosted and served by Google Play's CDN — no infrastructure cost or maintenance.

2. **Resumable downloads**: the Play Core SDK handles resume, retry, and background download automatically. We do not implement download management logic.

3. **Conditional delivery**: PAD supports device targeting. In a future release, we can deliver the Gemma 4 pack only to devices with sufficient RAM (≥8 GB), avoiding unnecessary downloads on low-end devices.

4. **On-demand delivery**: models are downloaded when the user explicitly requests them (during onboarding or from Settings), not at install time. This keeps the initial install fast.

5. **Integration with LiteRT-LM**: LiteRT-LM reads model files from paths on internal storage. PAD delivers asset packs to a stable path that persists across app updates.

6. **Play Integrity**: assets delivered via PAD are integrity-checked by Google Play, reducing the risk of tampered model files on sideloaded builds.

## Trade-offs

- **Play Store dependency**: PAD only works with Google Play distribution. Sideloaded APKs (e.g., from GitHub Releases) cannot use PAD and fall back to the Light mode (static dataset). This is acceptable given our target audience.
- **Testing complexity**: PAD requires a published internal track to test the full download flow. Local development uses a bundled fallback dataset (Light mode) or manually placed model files.
- **Quota and policy**: Google Play enforces asset pack policies. Model files must comply with Play's content policies.

## Implementation Notes

- Each model is a separate `on-demand` asset pack:
  - `gemma4_e2b_pack` — Gemma 4 E2B (~2 GB)
  - `gemma3_1b_pack` — Gemma 3 1B (~1 GB)
- Download state is tracked via `StateMachine` in `feature/onboarding` and `feature/settings`.
- `WorkManager` polls download progress and updates `DownloadState` in Room.
- On non-Play builds, the app detects missing packs and activates Light mode automatically.

## Consequences

- The `feature/onboarding` and `feature/settings` modules depend on Play Core Asset Delivery SDK.
- `core/ai` is isolated from delivery concerns — it only needs a file path to the model.
- Any change to model hosting strategy requires revisiting this ADR.
